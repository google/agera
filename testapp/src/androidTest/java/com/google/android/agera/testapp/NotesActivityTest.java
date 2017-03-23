/*
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.agera.testapp;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.clearText;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.longClick;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.matcher.RootMatchers.isDialog;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class NotesActivityTest {
  @Rule
  public ActivityTestRule<NotesActivity> activityRule = new ActivityTestRule<>(NotesActivity.class);
  public static final String FIRST_TEXT = "First Text";
  public static final String COL_FIRST_TEXT = "F First Text";
  public static final String SECOND_TEXT = "Second Text";
  public static final String COL_SECOND_TEXT = "S Second Text";
  public static final String THIRD_TEXT = "Third Text";

  @Test
  public void sanityTest() {
    onView(withId(R.id.clear)).perform(click());
    onView(withId(R.id.add)).perform(click());
    onView(withId(R.id.edit)).perform(clearText(), typeText(FIRST_TEXT));
    onView(withText(R.string.add)).inRoot(isDialog()).perform(click());
    onView(withId(R.id.add)).perform(click());
    onView(withId(R.id.edit)).perform(clearText(), typeText(SECOND_TEXT));
    onView(withText(R.string.add)).inRoot(isDialog()).perform(click());
    onView(withText(COL_FIRST_TEXT)).perform(click());
    onView(withId(R.id.edit)).perform(clearText(), typeText(THIRD_TEXT));
    onView(withText(R.string.edit)).inRoot(isDialog()).perform(click());
    onView(withText(COL_SECOND_TEXT)).perform(longClick());
    onView(withId(R.id.clear)).perform(click());
  }
}