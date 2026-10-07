from datetime import datetime, timezone
from uuid import uuid4

from sqlalchemy import Boolean, DateTime, ForeignKey, Integer, String, Text, UniqueConstraint
from sqlalchemy.orm import Mapped, mapped_column

from app.database import Base


def new_id() -> str:
    return str(uuid4())


def utc_now() -> datetime:
    return datetime.now(timezone.utc)


class ClassRoom(Base):
    __tablename__ = "classes"

    id: Mapped[str] = mapped_column(String(64), primary_key=True, default=new_id)
    class_code: Mapped[str] = mapped_column(String(32), unique=True, index=True)
    name: Mapped[str] = mapped_column(String(160))
    department: Mapped[str] = mapped_column(String(160), default="Computer Science")
    institution: Mapped[str] = mapped_column(String(160), default="Skill Discovery")


def random_contact_number() -> str:
    import random
    return str(random.randint(6000000000, 9999999999))


class User(Base):
    __tablename__ = "users"

    id: Mapped[str] = mapped_column(String(64), primary_key=True, default=new_id)
    name: Mapped[str] = mapped_column(String(160))
    email: Mapped[str] = mapped_column(String(320), unique=True, index=True)
    password_hash: Mapped[str] = mapped_column(String(255))
    student_id: Mapped[str] = mapped_column(String(80), default="")
    contact_number: Mapped[str] = mapped_column(String(20), default=random_contact_number)
    class_id: Mapped[str] = mapped_column(ForeignKey("classes.id"), index=True)
    avatar_url: Mapped[str] = mapped_column(String(500), default="")
    bio: Mapped[str] = mapped_column(Text, default="")
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utc_now)


class Achievement(Base):
    __tablename__ = "achievements"

    id: Mapped[str] = mapped_column(String(64), primary_key=True, default=new_id)
    student_id: Mapped[str] = mapped_column(ForeignKey("users.id"), index=True)
    title: Mapped[str] = mapped_column(String(240))
    description: Mapped[str] = mapped_column(Text)
    date: Mapped[str] = mapped_column(String(32))
    category: Mapped[str] = mapped_column(String(100))
    proof_url: Mapped[str] = mapped_column(String(500), default="")
    visibility: Mapped[str] = mapped_column(String(32), default="all")
    extracted_skills_json: Mapped[str] = mapped_column(Text, default="[]")
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utc_now)


class Skill(Base):
    __tablename__ = "skills"
    __table_args__ = (UniqueConstraint("user_id", "name", name="uq_skill_user_name"),)

    id: Mapped[str] = mapped_column(String(64), primary_key=True, default=new_id)
    user_id: Mapped[str] = mapped_column(ForeignKey("users.id"), index=True)
    name: Mapped[str] = mapped_column(String(120))
    category: Mapped[str] = mapped_column(String(100), default="General")
    level: Mapped[int] = mapped_column(Integer, default=50)
    proficiency: Mapped[str] = mapped_column(String(40), default="Intermediate")


class SkillEdge(Base):
    __tablename__ = "skill_edges"
    __table_args__ = (UniqueConstraint("user_id", "source", "target", name="uq_skill_edge"),)

    id: Mapped[str] = mapped_column(String(64), primary_key=True, default=new_id)
    user_id: Mapped[str] = mapped_column(ForeignKey("users.id"), index=True)
    source: Mapped[str] = mapped_column(String(120))
    target: Mapped[str] = mapped_column(String(120))
    weight: Mapped[int] = mapped_column(Integer, default=1)


class ActivityEvent(Base):
    __tablename__ = "activity_events"

    id: Mapped[str] = mapped_column(String(64), primary_key=True, default=new_id)
    user_id: Mapped[str] = mapped_column(ForeignKey("users.id"), index=True)
    event_type: Mapped[str] = mapped_column(String(80))
    event_date: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utc_now, index=True)
    points: Mapped[int] = mapped_column(Integer, default=1)


class Discussion(Base):
    __tablename__ = "discussions"

    id: Mapped[str] = mapped_column(String(64), primary_key=True, default=new_id)
    author_id: Mapped[str] = mapped_column(ForeignKey("users.id"), index=True)
    class_id: Mapped[str] = mapped_column(ForeignKey("classes.id"), index=True)
    title: Mapped[str] = mapped_column(String(240))
    description: Mapped[str] = mapped_column(Text)
    tags_json: Mapped[str] = mapped_column(Text, default="[]")
    visibility: Mapped[str] = mapped_column(String(32), default="class")
    poll_question: Mapped[str] = mapped_column(String(300), default="")
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utc_now, index=True)


class PollOption(Base):
    __tablename__ = "poll_options"

    id: Mapped[str] = mapped_column(String(64), primary_key=True, default=new_id)
    discussion_id: Mapped[str] = mapped_column(ForeignKey("discussions.id", ondelete="CASCADE"), index=True)
    text: Mapped[str] = mapped_column(String(240))
    position: Mapped[int] = mapped_column(Integer)


class PollVote(Base):
    __tablename__ = "poll_votes"
    __table_args__ = (UniqueConstraint("discussion_id", "user_id", name="uq_poll_vote_user"),)

    id: Mapped[str] = mapped_column(String(64), primary_key=True, default=new_id)
    discussion_id: Mapped[str] = mapped_column(ForeignKey("discussions.id", ondelete="CASCADE"), index=True)
    option_id: Mapped[str] = mapped_column(ForeignKey("poll_options.id", ondelete="CASCADE"))
    user_id: Mapped[str] = mapped_column(ForeignKey("users.id"), index=True)


class DiscussionUpvote(Base):
    __tablename__ = "discussion_upvotes"
    __table_args__ = (UniqueConstraint("discussion_id", "user_id", name="uq_upvote_user"),)

    id: Mapped[str] = mapped_column(String(64), primary_key=True, default=new_id)
    discussion_id: Mapped[str] = mapped_column(ForeignKey("discussions.id", ondelete="CASCADE"), index=True)
    user_id: Mapped[str] = mapped_column(ForeignKey("users.id"), index=True)
