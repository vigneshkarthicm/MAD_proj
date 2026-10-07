import json
from collections import defaultdict
from datetime import datetime, timedelta, timezone
from typing import Any

from fastapi import APIRouter, Depends, Query
from sqlalchemy import func, or_, select
from sqlalchemy.orm import Session

from app.auth import api_error, create_access_token, current_user, hash_password, verify_password
from app.database import get_db
from app.models import (
    Achievement, ActivityEvent, ClassRoom, Discussion, DiscussionUpvote, PollOption, PollVote,
    Skill, SkillEdge, User, new_id, utc_now,
)
from app.schemas import AchievementRequest, DiscussionEditRequest, DiscussionRequest, LoginRequest, RegisterRequest, SkillExtractRequest, SkillPayload, VoteRequest
from app.skills import extract_skills

router = APIRouter()


def skill_dict(skill: Skill) -> dict[str, Any]:
    return {"name": skill.name, "category": skill.category, "level": skill.level, "proficiency": skill.proficiency}


def user_dict(user: User, db: Session) -> dict[str, Any]:
    skills = db.scalars(select(Skill).where(Skill.user_id == user.id).order_by(Skill.name)).all()
    classroom = db.get(ClassRoom, user.class_id)
    contact = getattr(user, "contact_number", None) or "9876543210"
    return {"id": user.id, "name": user.name, "email": user.email, "student_id": user.student_id, "class_id": user.class_id,
            "class_name": classroom.name if classroom else "", "avatar_url": user.avatar_url, "bio": user.bio,
            "contact_number": contact,
            "skills": [skill_dict(skill) for skill in skills]}


def class_dict(classroom: ClassRoom, db: Session) -> dict[str, Any]:
    total = db.scalar(select(func.count(User.id)).where(User.class_id == classroom.id)) or 0
    return {"id": classroom.id, "class_code": classroom.class_code, "name": classroom.name,
            "department": classroom.department, "institution": classroom.institution, "total_members": total}


def visible_discussion(discussion: Discussion, user: User) -> bool:
    return discussion.visibility in {"all", "public"} or discussion.class_id == user.class_id or discussion.author_id == user.id


def visibility_percentage(discussion: Discussion, db: Session) -> int:
    eligible_query = select(User.id)
    if discussion.visibility == "class":
        eligible_query = eligible_query.where(User.class_id == discussion.class_id)
    eligible_count = db.scalar(select(func.count()).select_from(eligible_query.subquery())) or 0
    if eligible_count == 0:
        return 0

    upvoters = set(db.scalars(select(DiscussionUpvote.user_id).where(DiscussionUpvote.discussion_id == discussion.id)).all())
    voters = set(db.scalars(select(PollVote.user_id).where(PollVote.discussion_id == discussion.id)).all())
    return round(len(upvoters | voters) / eligible_count * 100)


def discussion_dict(discussion: Discussion, user: User, db: Session) -> dict[str, Any]:
    options = db.scalars(select(PollOption).where(PollOption.discussion_id == discussion.id).order_by(PollOption.position)).all()
    counts = dict(db.execute(select(PollVote.option_id, func.count(PollVote.id)).where(PollVote.discussion_id == discussion.id).group_by(PollVote.option_id)).all())
    vote = db.scalar(select(PollVote).where(PollVote.discussion_id == discussion.id, PollVote.user_id == user.id))
    author = db.get(User, discussion.author_id)
    upvotes = db.scalar(select(func.count(DiscussionUpvote.id)).where(DiscussionUpvote.discussion_id == discussion.id)) or 0
    return {"id": discussion.id, "title": discussion.title, "description": discussion.description, "author_id": discussion.author_id,
            "author_name": author.name if author else "", "author_avatar": author.avatar_url if author else "",
            "tags": json.loads(discussion.tags_json), "created_at": int(discussion.created_at.timestamp() * 1000),
            "upvotes_count": upvotes, "user_has_upvoted": db.scalar(select(DiscussionUpvote.id).where(DiscussionUpvote.discussion_id == discussion.id, DiscussionUpvote.user_id == user.id)) is not None,
            "poll_question": discussion.poll_question,
            "poll_options": [{"text": option.text, "votes": counts.get(option.id, 0)} for option in options],
            "user_voted_option_index": next((option.position for option in options if vote and option.id == vote.option_id), -1),
            "visibility_percentage": visibility_percentage(discussion, db), "class_id": discussion.class_id, "visibility": discussion.visibility}


def achievement_dict(achievement: Achievement, db: Session) -> dict[str, Any]:
    extracted_skills = json.loads(achievement.extracted_skills_json or "[]")
    return {"id": achievement.id, "student_id": achievement.student_id, "title": achievement.title, "description": achievement.description,
            "date": achievement.date, "category": achievement.category, "proof_url": achievement.proof_url,
            "extracted_skills": extracted_skills, "visibility": achievement.visibility,
            "created_at": int(achievement.created_at.timestamp() * 1000)}


def normalize_skill(item: object) -> dict[str, object] | None:
    if not isinstance(item, dict):
        return None
    name = " ".join(str(item.get("name", "")).casefold().split())
    if not name:
        return None
    try:
        level = max(0, min(100, int(item.get("level", 50))))
    except (TypeError, ValueError):
        level = 50
    normalized = SkillPayload(
        name=name,
        category=str(item.get("category", "General") or "General").strip() or "General",
        level=level,
        proficiency=str(item.get("proficiency", "Intermediate") or "Intermediate").strip() or "Intermediate",
    )
    return normalized.model_dump()


@router.post("/auth/register")
def register(request: RegisterRequest, db: Session = Depends(get_db)) -> dict[str, Any]:
    if db.scalar(select(User).where(User.email == request.email.lower().strip())):
        raise api_error("EMAIL_EXISTS", "An account with this email already exists.", 409)
    classroom = db.scalar(select(ClassRoom).where(ClassRoom.class_code == request.class_code.upper().strip()))
    if not classroom:
        raise api_error("INVALID_CLASS", "The class code is not valid.", 400)
    student_id = request.student_id.strip() if request.student_id.strip() else f"STU-{new_id()[:8].upper()}"
    user = User(name=request.name.strip(), email=request.email.lower().strip(), password_hash=hash_password(request.password),
                class_id=classroom.id, student_id=student_id)
    db.add(user)
    db.commit()
    db.refresh(user)
    return {"token": create_access_token(user.id), "user": user_dict(user, db)}


@router.post("/auth/login")
def login(request: LoginRequest, db: Session = Depends(get_db)) -> dict[str, Any]:
    identifier = request.email.lower().strip()
    user = db.scalar(select(User).where(or_(func.lower(User.email) == identifier, func.lower(User.student_id) == identifier)))
    if not user or not verify_password(request.password, user.password_hash):
        raise api_error("INVALID_CREDENTIALS", "Email/Hardware Student ID or password is incorrect.", 401)
    return {"token": create_access_token(user.id), "user": user_dict(user, db)}


@router.get("/users/me")
def get_me(user: User = Depends(current_user), db: Session = Depends(get_db)) -> dict[str, Any]:
    return user_dict(user, db)


@router.get("/users/{user_id}")
def get_user(user_id: str, user: User = Depends(current_user), db: Session = Depends(get_db)) -> dict[str, Any]:
    target = db.get(User, user_id)
    if not target or target.class_id != user.class_id:
        raise api_error("NOT_FOUND", "User was not found.", 404)
    return user_dict(target, db)


@router.get("/classes/{class_id}")
def get_class(class_id: str, user: User = Depends(current_user), db: Session = Depends(get_db)) -> dict[str, Any]:
    classroom = db.get(ClassRoom, class_id)
    if not classroom or classroom.id != user.class_id:
        raise api_error("FORBIDDEN", "You are not a member of this class.", 403)
    return class_dict(classroom, db)


@router.post("/classes/join")
def join_class(request: dict[str, str], user: User = Depends(current_user), db: Session = Depends(get_db)) -> dict[str, Any]:
    classroom = db.scalar(select(ClassRoom).where(ClassRoom.class_code == request.get("class_code", "").upper().strip()))
    if not classroom:
        raise api_error("INVALID_CLASS", "The class code is not valid.", 400)
    user.class_id = classroom.id
    db.commit()
    return class_dict(classroom, db)


@router.get("/achievements")
def list_achievements(student_id: str | None = None, user: User = Depends(current_user), db: Session = Depends(get_db)) -> list[dict[str, Any]]:
    owner_id = student_id or user.id
    achievements = db.scalars(select(Achievement).where(Achievement.student_id == owner_id).order_by(Achievement.created_at.desc())).all()
    return [achievement_dict(item, db) for item in achievements if item.visibility == "all" or owner_id == user.id]


@router.post("/achievements")
async def create_achievement(request: AchievementRequest, user: User = Depends(current_user), db: Session = Depends(get_db)) -> dict[str, Any]:
    if request.visibility not in {"all", "user", "groups"}:
        raise api_error("INVALID_VISIBILITY", "Visibility must be all, user, or groups.")
    extracted = [skill for item in await extract_skills(f"{request.title}. {request.description}") if (skill := normalize_skill(item))]
    achievement = Achievement(student_id=user.id, title=request.title.strip(), description=request.description.strip(), date=request.date,
                              category=request.category, proof_url=request.proof_url, visibility=request.visibility,
                              extracted_skills_json=json.dumps(extracted))
    db.add(achievement)
    db.flush()
    skill_names: list[str] = []
    for item in extracted:
        name = str(item["name"]).strip()
        canonical_name = name.casefold()
        if canonical_name in skill_names:
            continue
        skill_names.append(canonical_name)
        skill = db.scalar(select(Skill).where(Skill.user_id == user.id, func.lower(Skill.name) == canonical_name))
        if skill:
            skill.level = min(100, skill.level + 5)
        else:
            skill = Skill(user_id=user.id, name=name, category=str(item["category"]), level=int(item["level"]), proficiency=str(item["proficiency"]))
            db.add(skill)
        for previous in skill_names[:-1]:
            if not db.scalar(select(SkillEdge).where(SkillEdge.user_id == user.id, SkillEdge.source == previous, SkillEdge.target == canonical_name)):
                db.add(SkillEdge(user_id=user.id, source=previous, target=canonical_name))
    db.add(ActivityEvent(user_id=user.id, event_type="achievement", points=3))
    db.commit()
    db.refresh(achievement)
    return achievement_dict(achievement, db)


@router.post("/skills/extract")
async def preview_skills(request: SkillExtractRequest, user: User = Depends(current_user)) -> dict[str, Any]:
    return {"skills": await extract_skills(request.achievement_text)}


@router.get("/users/me/metrics")
def metrics(user: User = Depends(current_user), db: Session = Depends(get_db)) -> dict[str, Any]:
    skills = db.scalars(select(Skill).where(Skill.user_id == user.id).order_by(Skill.level.desc())).all()
    edges = db.scalars(select(SkillEdge).where(SkillEdge.user_id == user.id)).all()
    since = datetime.now(timezone.utc) - timedelta(days=7)
    events = db.scalars(select(ActivityEvent).where(ActivityEvent.user_id == user.id, ActivityEvent.event_date >= since)).all()
    days = {event.event_date.date().isoformat() for event in events}
    return {"skills": [skill_dict(skill) for skill in skills], "skill_graph": {"nodes": [skill_dict(skill) for skill in skills], "edges": [{"source": edge.source, "target": edge.target, "weight": edge.weight} for edge in edges]},
            "weekly_consistency": len(days), "weekly_activity": [{"date": day, "points": sum(event.points for event in events if event.event_date.date().isoformat() == day)} for day in sorted(days)]}


@router.get("/users/me/skill-graph")
def skill_graph(user: User = Depends(current_user), db: Session = Depends(get_db)) -> dict[str, Any]:
    return metrics(user, db)["skill_graph"]


@router.get("/users/me/activity")
def activity(period: str = Query("week"), user: User = Depends(current_user), db: Session = Depends(get_db)) -> dict[str, Any]:
    return metrics(user, db)["weekly_activity"]


@router.get("/explore/students")
def explore_students(query: str | None = None, skill: str | None = None, class_id: str | None = None, user: User = Depends(current_user), db: Session = Depends(get_db)) -> list[dict[str, Any]]:
    target_class = class_id or user.class_id
    students = db.scalars(select(User).where(User.class_id == target_class, User.id != user.id)).all()
    result = []
    for student in students:
        profile = user_dict(student, db)
        if query and query.lower() not in student.name.lower():
            continue
        if skill and not any(skill.lower() in item["name"].lower() for item in profile["skills"]):
            continue
        profile["skills"] = [item["name"] for item in profile["skills"]]
        profile["match_score"] = 85
        result.append(profile)
    return result


@router.get("/discussions")
def list_discussions(tag: str | None = None, class_id: str | None = None, user: User = Depends(current_user), db: Session = Depends(get_db)) -> list[dict[str, Any]]:
    visibility_filter = or_(Discussion.visibility == "all", Discussion.class_id == user.class_id)
    if class_id:
        visibility_filter = or_(Discussion.visibility == "all", Discussion.class_id == class_id)
    discussions = db.scalars(select(Discussion).where(visibility_filter).order_by(Discussion.created_at.desc())).all()
    return [discussion_dict(item, user, db) for item in discussions if visible_discussion(item, user) and (not tag or tag in json.loads(item.tags_json))]


@router.post("/discussions")
def create_discussion(request: DiscussionRequest, user: User = Depends(current_user), db: Session = Depends(get_db)) -> dict[str, Any]:
    class_id = request.class_id or user.class_id
    if class_id != user.class_id:
        raise api_error("FORBIDDEN", "You can only post in your class.", 403)
    if request.visibility not in {"all", "class"}:
        raise api_error("INVALID_VISIBILITY", "Discussion visibility must be all or class.")
    cleaned_poll_options = [option.strip() for option in request.poll_options if option.strip()]
    if cleaned_poll_options and not request.poll_question.strip():
        raise api_error("INVALID_POLL", "A poll question is required when poll options are provided.")
    if len(cleaned_poll_options) == 1:
        raise api_error("INVALID_POLL", "A poll must have at least two options.")
    discussion = Discussion(author_id=user.id, class_id=class_id, title=request.title.strip(), description=request.description.strip(),
                            tags_json=json.dumps(request.tags), visibility=request.visibility, poll_question=request.poll_question.strip())
    db.add(discussion)
    db.flush()
    for position, text in enumerate(cleaned_poll_options):
        db.add(PollOption(discussion_id=discussion.id, text=text, position=position))
    db.add(ActivityEvent(user_id=user.id, event_type="discussion", points=1))
    db.commit()
    db.refresh(discussion)
    return discussion_dict(discussion, user, db)


@router.post("/discussions/{discussion_id}/upvote")
def toggle_upvote(discussion_id: str, user: User = Depends(current_user), db: Session = Depends(get_db)) -> dict[str, Any]:
    discussion = db.get(Discussion, discussion_id)
    if not discussion or not visible_discussion(discussion, user):
        raise api_error("NOT_FOUND", "Discussion was not found.", 404)
    existing = db.scalar(select(DiscussionUpvote).where(DiscussionUpvote.discussion_id == discussion_id, DiscussionUpvote.user_id == user.id))
    if existing:
        db.delete(existing)
    else:
        db.add(DiscussionUpvote(discussion_id=discussion_id, user_id=user.id))
    db.commit()
    updated = discussion_dict(discussion, user, db)
    return {"discussion_id": discussion_id, "upvotes_count": updated["upvotes_count"], "user_has_upvoted": updated["user_has_upvoted"]}


def update_vote(discussion_id: str, request: VoteRequest | None, user: User, db: Session) -> dict[str, Any]:
    discussion = db.get(Discussion, discussion_id)
    if not discussion or not visible_discussion(discussion, user):
        raise api_error("NOT_FOUND", "Discussion was not found.", 404)
    options = db.scalars(select(PollOption).where(PollOption.discussion_id == discussion_id).order_by(PollOption.position)).all()
    if not options:
        raise api_error("NO_POLL", "This discussion has no poll.")
    existing = db.scalar(select(PollVote).where(PollVote.discussion_id == discussion_id, PollVote.user_id == user.id))
    if request is None:
        if existing:
            db.delete(existing)
    else:
        if request.option_index < 0 or request.option_index >= len(options):
            raise api_error("INVALID_OPTION", "That poll option does not exist.")
        if existing:
            existing.option_id = options[request.option_index].id
        else:
            db.add(PollVote(discussion_id=discussion_id, option_id=options[request.option_index].id, user_id=user.id))
    db.commit()
    return discussion_dict(discussion, user, db)


@router.post("/discussions/{discussion_id}/vote")
def vote(discussion_id: str, request: VoteRequest, user: User = Depends(current_user), db: Session = Depends(get_db)) -> dict[str, Any]:
    result = update_vote(discussion_id, request, user, db)
    return {"discussion_id": discussion_id, "poll_options": result["poll_options"], "user_voted_option_index": result["user_voted_option_index"], "visibility_percentage": result["visibility_percentage"]}


@router.delete("/discussions/{discussion_id}/vote")
def clear_vote(discussion_id: str, user: User = Depends(current_user), db: Session = Depends(get_db)) -> dict[str, Any]:
    result = update_vote(discussion_id, None, user, db)
    return {"discussion_id": discussion_id, "poll_options": result["poll_options"], "user_voted_option_index": result["user_voted_option_index"], "visibility_percentage": result["visibility_percentage"]}


@router.put("/discussions/{discussion_id}")
def edit_discussion(discussion_id: str, request: DiscussionEditRequest, user: User = Depends(current_user), db: Session = Depends(get_db)) -> dict[str, Any]:
    discussion = db.get(Discussion, discussion_id)
    if not discussion or not visible_discussion(discussion, user):
        raise api_error("NOT_FOUND", "Discussion was not found.", 404)

    if request.title is not None:
        discussion.title = request.title.strip()
    if request.description is not None:
        discussion.description = request.description.strip()
    if request.tags is not None:
        discussion.tags_json = json.dumps(request.tags)
    if request.poll_question is not None:
        discussion.poll_question = request.poll_question.strip()

    if request.poll_options is not None:
        existing_options = db.scalars(select(PollOption).where(PollOption.discussion_id == discussion_id).order_by(PollOption.position)).all()
        for idx, opt_data in enumerate(request.poll_options):
            text = str(opt_data.get("text", "")).strip()
            if idx < len(existing_options):
                existing_options[idx].text = text
            else:
                db.add(PollOption(discussion_id=discussion_id, text=text, position=idx))

    if request.upvotes_count is not None:
        current_upvotes = db.scalars(select(DiscussionUpvote).where(DiscussionUpvote.discussion_id == discussion_id)).all()
        delta = request.upvotes_count - len(current_upvotes)
        if delta > 0:
            for _ in range(delta):
                db.add(DiscussionUpvote(discussion_id=discussion_id, user_id=f"user_{new_id()[:8]}"))
        elif delta < 0:
            for upvote_item in current_upvotes[:abs(delta)]:
                db.delete(upvote_item)

    db.commit()
    return discussion_dict(discussion, user, db)
