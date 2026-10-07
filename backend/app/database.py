from collections.abc import Generator

from sqlalchemy import create_engine, inspect, text
from sqlalchemy.orm import DeclarativeBase, Session, sessionmaker

from app.config import get_settings


class Base(DeclarativeBase):
    pass


settings = get_settings()
connect_args = {"check_same_thread": False} if settings.database_url.startswith("sqlite") else {}
engine = create_engine(settings.database_url, connect_args=connect_args, future=True)
SessionLocal = sessionmaker(bind=engine, autoflush=False, expire_on_commit=False)


def get_db() -> Generator[Session, None, None]:
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


def ensure_schema_compatibility() -> None:
    inspector = inspect(engine)
    if inspector.has_table("achievements"):
        achievement_columns = {column["name"] for column in inspector.get_columns("achievements")}
        if "extracted_skills_json" not in achievement_columns:
            with engine.begin() as connection:
                connection.execute(text("ALTER TABLE achievements ADD COLUMN extracted_skills_json TEXT NOT NULL DEFAULT '[]'"))

    if inspector.has_table("users"):
        user_columns = {column["name"] for column in inspector.get_columns("users")}
        if "contact_number" not in user_columns:
            with engine.begin() as connection:
                connection.execute(text("ALTER TABLE users ADD COLUMN contact_number VARCHAR(20) NOT NULL DEFAULT '9876543210'"))
