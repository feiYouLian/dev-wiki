package com.wins.utils;


import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;

public class EmailUtil {

    public static void main(String[] args) {
        // Recipient's email ID needs to be mentioned.
        String to = "602543887@qq.com";

        // Sender's email ID needs to be mentioned
        String from = "463553595@qq.com";

        final String username = "463553595@qq.com";//change accordingly
        final String password = "taswfyjkezznbhhe";//change accordingly
        // Assuming you are sending email through relay.jangosmtp.net
        String host = "smtp.qq.com";
        String protocol = "smtp";
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", "587");

        // Get the Session object.
        Session session = Session.getInstance(props,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });

        try {
            // Create a default MimeMessage object.
            Message message = new MimeMessage(session);

            // Set From: header field of the header.
            message.setFrom(new InternetAddress(from));

            // Set To: header field of the header.
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));

            // Set Subject: header field
            message.setSubject("Testing Subject");

            // Create the message part
            BodyPart messageBodyPart = new MimeBodyPart();

            // Now set the actual message
            messageBodyPart.setText("This is message body");

            // Create a multipar message
            Multipart multipart = new MimeMultipart();

            // Set text message part
            multipart.addBodyPart(messageBodyPart);

            // Part two is attachment
            messageBodyPart = new MimeBodyPart();
            FileDataSource fds = new FileDataSource("E:\\chromeDownload\\工作日报-20200219-费优连.xlsx"); //得到数据源
            messageBodyPart.setDataHandler(new DataHandler(fds)); //得到附件本身并放入BodyPart
            messageBodyPart.setFileName(MimeUtility.encodeText(fds.getName()));  //得到文件名并编码（防止中文文件名乱码）同样放入BodyPart

            multipart.addBodyPart(messageBodyPart);

            // Send the complete message parts
            message.setContent(multipart);

            // Send message
//            Transport.send(message);
            Transport t = session.getTransport(protocol);
            try {
                t.connect();
                message.saveChanges();
                t.sendMessage(message, message.getAllRecipients());
            } finally {
                t.close();
            }
            System.out.println("MessageId:" + ((MimeMessage) message).getMessageID());
            System.out.println("Sent message successfully....");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}
