package com.mbcu.mmm.sequences;

public enum LogNum {

	CANCELED_UNFUNDED ("Offer canceled, no longer funded"),
	CANCELED_OFFERCANCEL("Canceled by OfferCancel");
	
  private final String text;

  private LogNum(final String text) {
      this.text = text;
  }

  @Override
  public String toString() {
      return text;
  }
	
	
}
