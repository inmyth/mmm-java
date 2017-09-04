package com.mbcu.mmm.notice;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.Message;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;
import com.mbcu.mmm.models.internal.Config;
import com.mbcu.mmm.sequences.Notifier;

public class SenderSES {
	
  private static final String FROM = "mbcutama@vauldex.com";
  private static Regions region = Regions.US_EAST_1;


  public static final void send (Config config, Logger logger, Notifier.RequestEmailNotice e) {
  	String body 	= bodyBotError(e);
  	String title 	= titleBotError(config);
  	config.getEmails().forEach(to -> {send(logger, title, body, to);});
  }
  
  private static final void send(Logger logger, String title, String body, String to) {
    try {
      AmazonSimpleEmailService client =  AmazonSimpleEmailServiceClientBuilder.standard()
            .withRegion(region)
            .build();
      SendEmailRequest request = new SendEmailRequest()
          .withDestination(new Destination().withToAddresses(to))
          .withMessage(new Message()
              .withBody(new Body()
                  .withText(new Content()
                      .withCharset("UTF-8").withData(body)))
              .withSubject(new Content()
                  .withCharset("UTF-8").withData(title)))
          .withSource(FROM);
      client.sendEmail(request);
      if (logger != null){
        logger.log(Level.FINER, "Email sent!");
      }
    } catch (Exception ex) {
      logger.log(Level.SEVERE, "The email was not sent. Error message: "  + ex.getMessage());
    }
  }
  
  public static final String bodyBotError(Notifier.RequestEmailNotice e){
  	StringBuilder sb = new StringBuilder("Currency Pair : ");
  	sb.append(e.pair);
  	sb.append("\n");
  	sb.append("Error : ");
  	sb.append(e.error);
  	return sb.toString();
  }
  
  public static final String titleBotError(Config config){
  	StringBuilder sb = new StringBuilder("Bot Error, Account ");
  	sb.append(config.getCredentials().getAddress());
  	return sb.toString();
  }
  
  
}


