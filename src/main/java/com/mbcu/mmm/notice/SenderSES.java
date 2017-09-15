package com.mbcu.mmm.notice;

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
	
  private static Regions region = Regions.US_EAST_1;
  private Config config;
  private Logger logger;
  
  
  public SenderSES (Config config, Logger logger) {
  	this.config = config;
  	this.logger = logger;	
  }


  public void sendAccBalance(String body){
  	String title = titleAccBalance(config);
  	config.getEmails().forEach(to -> {send(logger, title, body, to);});
  }
  
   
  public void sendBotError (Notifier.RequestEmailNotice e) {
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
          .withSource(to);
      client.sendEmail(request);
      if (logger != null){
        logger.log(Level.FINER, "Email sent!");
      }
    } catch (Exception ex) {
      logger.log(Level.SEVERE, "The email was not sent. Error message: "  + ex.getMessage());
    }
  }
  
  private static String bodyBotError(Notifier.RequestEmailNotice e){
  	StringBuilder sb = new StringBuilder("Currency Pair : ");
  	sb.append(e.pair);
  	sb.append("\n");
  	sb.append("Error : ");
  	sb.append(e.error);
  	return sb.toString();
  }
  
  private static String titleBotError(Config config){
  	StringBuilder sb = new StringBuilder("Bot Error, Account ");
  	sb.append(config.getCredentials().getAddress());
  	return sb.toString();
  }
  
  private static String titleAccBalance(Config config){
   	StringBuilder sb = new StringBuilder("Account Balance of ");
  	sb.append(config.getCredentials().getAddress());
  	return sb.toString();
  }
  
  
}


