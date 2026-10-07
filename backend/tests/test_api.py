from fastapi.testclient import TestClient

from app.main import app


def test_health_and_poll_mutations() -> None:
    with TestClient(app) as client:
        health = client.get("/api/v1/health")
        assert health.status_code == 200

        login = client.post(
            "/api/v1/auth/login",
            json={"email": "demo@skilldiscovery.app", "password": "password123"},
        )
        assert login.status_code == 200
        headers = {"Authorization": f"Bearer {login.json()['token']}"}

        discussion = client.post(
            "/api/v1/discussions",
            headers=headers,
            json={
                "title": "Test poll",
                "description": "Test discussion",
                "poll_question": "Pick one",
                "poll_options": ["First", "Second"],
            },
        )
        assert discussion.status_code == 200
        discussion_id = discussion.json()["id"]

        first_upvote = client.post(f"/api/v1/discussions/{discussion_id}/upvote", headers=headers)
        second_upvote = client.post(f"/api/v1/discussions/{discussion_id}/upvote", headers=headers)
        assert first_upvote.json()["user_has_upvoted"] is True
        assert second_upvote.json()["user_has_upvoted"] is False

        vote = client.post(
            f"/api/v1/discussions/{discussion_id}/vote",
            headers=headers,
            json={"option_index": 1},
        )
        assert vote.status_code == 200
        assert [option["votes"] for option in vote.json()["poll_options"]] == [0, 1]
        invalid_vote = client.post(
            f"/api/v1/discussions/{discussion_id}/vote",
            headers=headers,
            json={"option_index": -1},
        )
        assert invalid_vote.status_code == 422
        cleared = client.delete(f"/api/v1/discussions/{discussion_id}/vote", headers=headers)
        assert cleared.status_code == 200
        assert cleared.json()["user_voted_option_index"] == -1

        discussion_list = client.get("/api/v1/discussions", headers=headers)
        synced = next(item for item in discussion_list.json() if item["id"] == discussion_id)
        assert synced["upvotes_count"] == 0
        assert [option["votes"] for option in synced["poll_options"]] == [0, 0]

        invalid_poll = client.post(
            "/api/v1/discussions",
            headers=headers,
            json={"title": "Invalid poll", "description": "One option", "poll_question": "Pick", "poll_options": ["Only"]},
        )
        assert invalid_poll.status_code == 422


def test_public_visibility_uses_distinct_engaged_users() -> None:
    with TestClient(app) as client:
        demo_login = client.post(
            "/api/v1/auth/login",
            json={"email": "demo@skilldiscovery.app", "password": "password123"},
        )
        demo_headers = {"Authorization": f"Bearer {demo_login.json()['token']}"}

        second_email = "visibility-test@example.com"
        registration = client.post(
            "/api/v1/auth/register",
            json={
                "name": "Visibility Tester",
                "email": second_email,
                "password": "password123",
                "class_code": "CSE2026",
            },
        )
        if registration.status_code == 409:
            registration = client.post(
                "/api/v1/auth/login",
                json={"email": second_email, "password": "password123"},
            )
        second_headers = {"Authorization": f"Bearer {registration.json()['token']}"}

        discussion = client.post(
            "/api/v1/discussions",
            headers=demo_headers,
            json={
                "title": "Public visibility test",
                "description": "Measure public engagement",
                "visibility": "all",
            },
        )
        assert discussion.status_code == 200, discussion.text
        discussion_id = discussion.json()["id"]
        assert discussion.json()["visibility_percentage"] == 0

        upvote = client.post(f"/api/v1/discussions/{discussion_id}/upvote", headers=second_headers)
        assert upvote.status_code == 200
        assert upvote.json()["user_has_upvoted"] is True

        discussions = client.get("/api/v1/discussions", headers=demo_headers)
        public_item = next(item for item in discussions.json() if item["id"] == discussion_id)
        assert public_item["visibility_percentage"] == 50


def test_achievement_skills_are_scoped_to_the_achievement() -> None:
    with TestClient(app) as client:
        login = client.post(
            "/api/v1/auth/login",
            json={"email": "demo@skilldiscovery.app", "password": "password123"},
        )
        headers = {"Authorization": f"Bearer {login.json()['token']}"}

        first = client.post(
            "/api/v1/achievements",
            headers=headers,
            json={
                "title": "Kotlin app",
                "description": "Built an Android app with Kotlin",
                "date": "2026-09-20",
                "category": "Project",
            },
        )
        second = client.post(
            "/api/v1/achievements",
            headers=headers,
            json={
                "title": "Leadership workshop",
                "description": "Completed a leadership workshop",
                "date": "2026-09-20",
                "category": "Learning",
            },
        )
        assert first.status_code == 200 and second.status_code == 200
        first_names = {skill["name"].casefold() for skill in first.json()["extracted_skills"]}
        second_names = {skill["name"].casefold() for skill in second.json()["extracted_skills"]}
        assert "kotlin" in first_names
        assert "leadership" in second_names
        assert "kotlin" not in second_names
