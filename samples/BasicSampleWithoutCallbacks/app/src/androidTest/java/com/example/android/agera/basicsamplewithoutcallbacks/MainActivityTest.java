/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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
package com.example.android.agera.basicsamplewithoutcallbacks;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.RootMatchers.withDecorView;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.not;

import android.support.test.espresso.ViewAction;
import android.support.test.espresso.action.GeneralLocation;
import android.support.test.espresso.action.GeneralSwipeAction;
import android.support.test.espresso.action.Press;
import android.support.test.espresso.action.Swipe;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for the main screen, which shows a list of usernames or an error message if the list
 * data couldn't be obtained.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class MainActivityTest {

  /**
   * {@link ActivityTestRule} is a JUnit {@link Rule @Rule} to launch your activity under test.
   * <p>
   * Rules are interceptors which are executed for each test method and are important building
   * blocks of Junit tests.
   */
  @Rule
  public ActivityTestRule<MainActivity> activityTestRule =
      new ActivityTestRule<>(MainActivity.class);

  /**
   * A custom {@link Matcher} which matches a {@link ListView} by its size.
   * <p>
   * View constraints:
   * <ul>
   * <li>View must be a {@link ListView}
   * <ul>
   *
   * @param size the expected size of the list
   * @return Matcher that matches size of the given {@link ListView}
   */
  private static Matcher<View> withListSize(final int size) {
    return new TypeSafeMatcher<View>() {
      @Override
      public boolean matchesSafely(final View view) {
        return ((ListView) view).getChildCount() == size;
      }

      @Override
      public void describeTo(final Description description) {
        description.appendText("ListView should have " + size + " items");
      }
    };
  }

  @Before
  public void setUp() {
    // Username fetcher in a state of returning 4 usernames
    UsernamesSupplier.NUMBER_OF_USERS = 4;
  }

  @After
  public void cleanUp() {
    // Username fetcher in a state of returning 4 usernames
    UsernamesSupplier.NUMBER_OF_USERS = 4;
  }

  @Test
  public void firstLoad_showsList() {
    // Then the correct number of usernames is displayed
    onView(withId(R.id.list)).check(matches(withListSize(UsernamesSupplier.NUMBER_OF_USERS)));
  }

  @Test
  public void refreshError_showsErrorAndPreviousList() {
    // Given a list of usernames loaded
    String firstUsername =
        ((TextView) ((ListView) activityTestRule.getActivity()
            .findViewById(R.id.list)).getChildAt(0)).getText().toString();

    // Given a username fetcher in a state of error
    UsernamesSupplier.NUMBER_OF_USERS = -1;

    // When pulled down to refresh is triggered
    onView(withId(R.id.fragment)).perform(swipeDown());

    // Then the error toast is shown
    onView(withText(R.string.error))
        // Required because of the way toasts are displayed
        .inRoot(withDecorView(not(is(
            activityTestRule.getActivity().getWindow().getDecorView()))))
        // Check if it is visible
        .check(matches(isDisplayed()));
    // And the list is still shown
    onView(withText(firstUsername)).check(matches(isDisplayed()));
  }

  @Test
  public void refresh_showsUpdatedList() {
    // Given a list of usernames loaded
    String firstUsername =
        ((TextView) ((ListView) activityTestRule.getActivity()
            .findViewById(R.id.list)).getChildAt(0)).getText().toString();

    // Given a username fetcher in a state of returning 5 usernames
    UsernamesSupplier.NUMBER_OF_USERS = 5;

    // When pulled down to refresh is triggered
    onView(withId(R.id.fragment)).perform(swipeDown());

    // Then the correct number of usernames is displayed
    onView(withId(R.id.list)).check(matches(withListSize(UsernamesSupplier.NUMBER_OF_USERS)));
  }

  private static ViewAction swipeDown() {
    return new GeneralSwipeAction(Swipe.FAST, GeneralLocation.TOP_CENTER,
        GeneralLocation.BOTTOM_CENTER, Press.FINGER);
  }
}
