from datetime import datetime
from typing import Any

from pydantic import BaseModel, Field, field_validator


class LoginRequest(BaseModel):
    email: str
    password: str


class RegisterRequest(BaseModel):
    name: str = Field(min_length=1, max_length=160)
    email: str
    password: str = Field(min_length=8)
    class_code: str = Field(min_length=1, max_length=32)
    student_id: str = Field(default="", max_length=80)


class SkillPayload(BaseModel):
    name: str
    category: str = "General"
    level: int = Field(default=50, ge=0, le=100)
    proficiency: str = "Intermediate"


class AchievementRequest(BaseModel):
    title: str = Field(min_length=1, max_length=240)
    description: str = Field(min_length=1)
    date: str
    category: str = Field(min_length=1, max_length=100)
    proof_url: str = ""
    visibility: str = "all"


class SkillExtractRequest(BaseModel):
    achievement_text: str = Field(min_length=1)


class DiscussionRequest(BaseModel):
    title: str = Field(min_length=1, max_length=240)
    description: str = Field(min_length=1)
    tags: list[str] = Field(default_factory=list)
    poll_question: str = ""
    poll_options: list[str] = Field(default_factory=list)
    class_id: str = ""
    visibility: str = "class"

    @field_validator("poll_options")
    @classmethod
    def validate_poll_options(cls, options: list[str]) -> list[str]:
        cleaned = [option.strip() for option in options if option.strip()]
        if options and (len(cleaned) < 2 or len(set(cleaned)) != len(cleaned)):
            raise ValueError("A poll needs at least two unique options.")
        return cleaned


class DiscussionEditRequest(BaseModel):
    title: str | None = None
    description: str | None = None
    tags: list[str] | None = None
    upvotes_count: int | None = Field(default=None, ge=0)
    poll_question: str | None = None
    poll_options: list[dict[str, Any]] | None = None


class VoteRequest(BaseModel):
    option_index: int = Field(ge=0)
