// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: src/main/java/org/dwallach/calwatch/proto/calendar.proto
package org.dwallach.calwatch.proto;

import com.squareup.wire.Message;
import com.squareup.wire.ProtoField;
import java.util.Collections;
import java.util.List;

import static com.squareup.wire.Message.Datatype.BOOL;
import static com.squareup.wire.Message.Datatype.INT32;
import static com.squareup.wire.Message.Label.REPEATED;
import static com.squareup.wire.Message.Label.REQUIRED;

public final class WireUpdate extends Message {

  public static final List<WireEvent> DEFAULT_EVENTS = Collections.emptyList();
  public static final Boolean DEFAULT_NEWEVENTS = false;
  public static final Integer DEFAULT_FACEMODE = 0;
  public static final Boolean DEFAULT_SHOWSECONDHAND = false;
  public static final Boolean DEFAULT_SHOWDAYDATE = false;

  @ProtoField(tag = 1, label = REPEATED)
  public final List<WireEvent> events;

  @ProtoField(tag = 2, type = BOOL, label = REQUIRED)
  public final Boolean newEvents;

  /**
   * true: the events are new, have a look; false: ignore the events field
   */
  @ProtoField(tag = 3, type = INT32, label = REQUIRED)
  public final Integer faceMode;

  @ProtoField(tag = 4, type = BOOL, label = REQUIRED)
  public final Boolean showSecondHand;

  @ProtoField(tag = 5, type = BOOL, label = REQUIRED)
  public final Boolean showDayDate;

  public WireUpdate(List<WireEvent> events, Boolean newEvents, Integer faceMode, Boolean showSecondHand, Boolean showDayDate) {
    this.events = immutableCopyOf(events);
    this.newEvents = newEvents;
    this.faceMode = faceMode;
    this.showSecondHand = showSecondHand;
    this.showDayDate = showDayDate;
  }

  private WireUpdate(Builder builder) {
    this(builder.events, builder.newEvents, builder.faceMode, builder.showSecondHand, builder.showDayDate);
    setBuilder(builder);
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (!(other instanceof WireUpdate)) return false;
    WireUpdate o = (WireUpdate) other;
    return equals(events, o.events)
        && equals(newEvents, o.newEvents)
        && equals(faceMode, o.faceMode)
        && equals(showSecondHand, o.showSecondHand)
        && equals(showDayDate, o.showDayDate);
  }

  @Override
  public int hashCode() {
    int result = hashCode;
    if (result == 0) {
      result = events != null ? events.hashCode() : 1;
      result = result * 37 + (newEvents != null ? newEvents.hashCode() : 0);
      result = result * 37 + (faceMode != null ? faceMode.hashCode() : 0);
      result = result * 37 + (showSecondHand != null ? showSecondHand.hashCode() : 0);
      result = result * 37 + (showDayDate != null ? showDayDate.hashCode() : 0);
      hashCode = result;
    }
    return result;
  }

  public static final class Builder extends Message.Builder<WireUpdate> {

    public List<WireEvent> events;
    public Boolean newEvents;
    public Integer faceMode;
    public Boolean showSecondHand;
    public Boolean showDayDate;

    public Builder() {
    }

    public Builder(WireUpdate message) {
      super(message);
      if (message == null) return;
      this.events = copyOf(message.events);
      this.newEvents = message.newEvents;
      this.faceMode = message.faceMode;
      this.showSecondHand = message.showSecondHand;
      this.showDayDate = message.showDayDate;
    }

    public Builder events(List<WireEvent> events) {
      this.events = checkForNulls(events);
      return this;
    }

    public Builder newEvents(Boolean newEvents) {
      this.newEvents = newEvents;
      return this;
    }

    /**
     * true: the events are new, have a look; false: ignore the events field
     */
    public Builder faceMode(Integer faceMode) {
      this.faceMode = faceMode;
      return this;
    }

    public Builder showSecondHand(Boolean showSecondHand) {
      this.showSecondHand = showSecondHand;
      return this;
    }

    public Builder showDayDate(Boolean showDayDate) {
      this.showDayDate = showDayDate;
      return this;
    }

    @Override
    public WireUpdate build() {
      checkRequiredFields();
      return new WireUpdate(this);
    }
  }
}
