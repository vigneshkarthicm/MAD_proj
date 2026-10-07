import re

import httpx

from app.config import get_settings

KNOWN_SKILLS = {
    "kotlin": ("Programming", 70),
    "java": ("Programming", 65),
    "python": ("Programming", 65),
    "android": ("Mobile Development", 70),
    "jetpack compose": ("Mobile Development", 75),
    "sql": ("Data", 60),
    "machine learning": ("AI", 65),
    "communication": ("Professional", 55),
    "leadership": ("Professional", 55),
}


def fallback_extract(text: str) -> list[dict[str, object]]:
    normalized = text.lower()
    found = [
        {"name": name.title(), "category": category, "level": level, "proficiency": "Intermediate"}
        for name, (category, level) in KNOWN_SKILLS.items()
        if name in normalized
    ]
    if found:
        return found
    words = [word for word in re.findall(r"[a-zA-Z][a-zA-Z+#.-]{2,}", text) if word.lower() not in {"the", "and", "with", "for"}]
    return [{"name": words[0].title(), "category": "General", "level": 50, "proficiency": "Intermediate"}] if words else []


async def extract_skills(text: str) -> list[dict[str, object]]:
    settings = get_settings()
    if not settings.gemini_api_key:
        return fallback_extract(text)

    prompt = (
        "Extract practical skills from this student achievement. Return only a JSON array "
        "of objects with name, category, level (0-100), and proficiency. Achievement: " + text
    )
    url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"
    try:
        async with httpx.AsyncClient(timeout=20) as client:
            response = await client.post(url, params={"key": settings.gemini_api_key}, json={"contents": [{"parts": [{"text": prompt}]}]})
            response.raise_for_status()
            body = response.json()
            generated = body["candidates"][0]["content"]["parts"][0]["text"]
            start, end = generated.find("["), generated.rfind("]")
            if start >= 0 and end > start:
                import json
                parsed = json.loads(generated[start:end + 1])
                return [item for item in parsed if isinstance(item, dict) and item.get("name")]
    except (httpx.HTTPError, KeyError, ValueError, TypeError):
        pass
    return fallback_extract(text)
