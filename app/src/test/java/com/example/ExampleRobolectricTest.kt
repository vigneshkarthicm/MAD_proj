package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Skiller", appName)
  }

  @Test
  fun `verify Room database initialization and seed data`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(
      context,
      com.example.data.local.SkillDiscoveryDatabase::class.java
    ).allowMainThreadQueries().build()

    val profileDao = db.studentDao()
    val achievementDao = db.achievementDao()

    val profile = com.example.data.local.entity.StudentProfileEntity(
      id = "test_user",
      name = "Test Student",
      email = "test@student.edu",
      studentId = "STU-001",
      classId = "cls_test",
      className = "Computer Science",
      avatarUrl = "",
      bio = "Test bio"
    )
    profileDao.insertProfile(profile)

    val fetched = profileDao.getProfile("test_user")
    org.junit.Assert.assertNotNull(fetched)

    val achievement = com.example.data.local.entity.AchievementEntity(
      id = "ach_test",
      studentId = "test_user",
      title = "ESP32 IoT Node",
      description = "Telemetry sensor",
      date = "2026-09-19",
      category = "Embedded",
      proofUrl = "https://github.com",
      extractedSkillsJson = "ESP32, C++",
      visibility = "all"
    )
    achievementDao.insertAchievement(achievement)

    db.close()
  }
}
