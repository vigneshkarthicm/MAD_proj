from sqlalchemy import select
from sqlalchemy.orm import Session

from app.auth import hash_password
from app.models import ClassRoom, Skill, User


def seed_defaults(db: Session) -> None:
    classroom = db.scalar(select(ClassRoom).where(ClassRoom.class_code == "CSE2026"))
    if classroom is None:
        classroom = ClassRoom(id="class_cse_2026", class_code="CSE2026", name="Computer Science 2026", department="Computer Science", institution="Skill Discovery")
        db.add(classroom)
        db.flush()
    demo = db.scalar(select(User).where(User.email == "demo@skilldiscovery.app"))
    if demo is None:
        demo = User(id="std_vinay", name="Vinay", email="demo@skilldiscovery.app", password_hash=hash_password("password123"), student_id="std_vinay", class_id=classroom.id)
        db.add(demo)
        db.flush()
        db.add_all([
            Skill(user_id=demo.id, name="Kotlin", category="Programming", level=78, proficiency="Advanced"),
            Skill(user_id=demo.id, name="Android", category="Mobile Development", level=72, proficiency="Advanced"),
        ])
    db.commit()
