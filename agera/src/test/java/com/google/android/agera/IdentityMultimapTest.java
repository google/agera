package com.google.android.agera;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.Before;
import org.junit.Test;

public final class IdentityMultimapTest {
  private static final String KEY_1 = "key1";
  private static final String KEY_2 = "key2";
  private static final String VALUE_1 = "value1";
  private static final String VALUE_2 = "value2";

  private IdentityMultimap identityMultimap;

  @Before
  public void setUp() {
    identityMultimap = new IdentityMultimap();
  }

  @Test
  public void shouldReturnThatNotAddedKeyWasNotRemoved() {
    assertThat(identityMultimap.removeKey(new Object()), is(false));
  }

  @Test
  public void shouldHandleRemovalOfNonAddedKeyValuePairWithoutKeyOrValue() {
    identityMultimap.removeKeyValuePair(new Object(), new Object());
  }

  @Test
  public void shouldReturnThatKeyValuePairWasAdded() {
    assertThat(identityMultimap.addKeyValuePair(KEY_1, VALUE_1), is(true));
  }

  @Test
  public void shouldReturnThatKeyValuePairWasNotAdded() {
    identityMultimap.addKeyValuePair(KEY_1, VALUE_1);

    assertThat(identityMultimap.addKeyValuePair(KEY_1, VALUE_1), is(false));
  }

  @Test
  public void shouldHandleRemovalOfAddedKeyValuePairWithoutKeyOrValue() {
    identityMultimap.addKeyValuePair(KEY_1, VALUE_1);

    identityMultimap.removeKeyValuePair(KEY_1, VALUE_1);
  }

  @Test
  public void shouldRemoveSecondValueForKeyOnRemoveKey() {
    identityMultimap.addKeyValuePair(KEY_1, VALUE_1);
    identityMultimap.addKeyValuePair(KEY_1, VALUE_2);
    identityMultimap.removeKeyValuePair(KEY_1, VALUE_1);

    assertThat(identityMultimap.removeKey(KEY_1), is(true));
    assertThat(identityMultimap.removeKey(KEY_1), is(false));
  }

  @Test
  public void shouldRemoveOnlyKeySpecified() {
    identityMultimap.addKeyValuePair(KEY_1, VALUE_1);
    identityMultimap.addKeyValuePair(KEY_2, VALUE_2);

    identityMultimap.removeKeyValuePair(KEY_1, VALUE_1);

    assertThat(identityMultimap.removeKey(KEY_2), is(true));
  }

  @Test
  public void shouldRemoveOnlyKeyValueSpecified() {
    identityMultimap.addKeyValuePair(KEY_1, VALUE_1);
    identityMultimap.addKeyValuePair(KEY_2, VALUE_2);

    identityMultimap.removeKey(KEY_1);

    assertThat(identityMultimap.removeKey(KEY_2), is(true));
  }

  @Test
  public void shouldHandleAddOfNewKeyValueAfterRemove() {
    identityMultimap.addKeyValuePair(KEY_1, VALUE_1);

    identityMultimap.removeKey(KEY_1);

    identityMultimap.addKeyValuePair(KEY_2, VALUE_2);

    assertThat(identityMultimap.removeKey(KEY_2), is(true));
  }
}
