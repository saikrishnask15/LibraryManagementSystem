-- MySQL dump 10.13  Distrib 8.0.44, for Win64 (x86_64)
--
-- Host: localhost    Database: librarymanagementsystem
-- ------------------------------------------------------
-- Server version	8.0.44

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `author`
--

DROP TABLE IF EXISTS `author`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `author` (
  `author_id` int NOT NULL AUTO_INCREMENT,
  `bio` varchar(1000) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  PRIMARY KEY (`author_id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `author`
--

LOCK TABLES `author` WRITE;
/*!40000 ALTER TABLE `author` DISABLE KEYS */;
INSERT INTO `author` VALUES (1,'string','Harper@gmail.com','Harper Lee'),(2,'American novelist','Jane@gmail.com','Jane Austen'),(3,'Software engineer','uncle.bob@example.com','Robert C. Martin'),(4,'ThoughtWorks','martin.fowler@example.com','Martin Fowler'),(5,'Google engineer','joshua.bloch@example.com','Joshua Bloch'),(7,'London novelist','Charlotte@gmail.com','Charlotte Brontë'),(8,NULL,'Boaz@gmail.com','Boaz Gavish'),(9,'string',NULL,'string');
/*!40000 ALTER TABLE `author` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `book`
--

DROP TABLE IF EXISTS `book`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `book` (
  `id` int NOT NULL AUTO_INCREMENT,
  `available` bit(1) DEFAULT NULL,
  `available_copies` int DEFAULT NULL,
  `is_bn` varchar(255) NOT NULL,
  `published_year` int DEFAULT NULL,
  `title` varchar(255) NOT NULL,
  `total_copies` int DEFAULT NULL,
  `author_id` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK4kkhudq55jcouaqnhpcoxmwqa` (`is_bn`),
  KEY `FKklnrv3weler2ftkweewlky958` (`author_id`),
  CONSTRAINT `FKklnrv3weler2ftkweewlky958` FOREIGN KEY (`author_id`) REFERENCES `author` (`author_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `book`
--

LOCK TABLES `book` WRITE;
/*!40000 ALTER TABLE `book` DISABLE KEYS */;
INSERT INTO `book` VALUES (1,_binary '',6,'12345678910',1960,'To Kill a Mockingbird',10,1),(2,_binary '\0',0,'12345678911',1813,'Pride and Prejudice',2,2),(3,_binary '\0',0,'978-013235088',2008,'Clean Code',3,2),(4,_binary '',3,'978-020148567',1999,'Refactoring',5,4),(5,_binary '',2,'32345678910',1848,'Jane Eyre - Charlotte',3,7),(8,_binary '',2,'42345678910',2026,'The Places I Love to Poop On',2,8);
/*!40000 ALTER TABLE `book` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `book_category`
--

DROP TABLE IF EXISTS `book_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `book_category` (
  `book_id` int NOT NULL,
  `category_id` int NOT NULL,
  KEY `FKam8llderp40mvbbwceqpu6l2s` (`category_id`),
  KEY `FKnyegcbpvce2mnmg26h0i856fd` (`book_id`),
  CONSTRAINT `FKam8llderp40mvbbwceqpu6l2s` FOREIGN KEY (`category_id`) REFERENCES `category` (`id`),
  CONSTRAINT `FKnyegcbpvce2mnmg26h0i856fd` FOREIGN KEY (`book_id`) REFERENCES `book` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `book_category`
--

LOCK TABLES `book_category` WRITE;
/*!40000 ALTER TABLE `book_category` DISABLE KEYS */;
INSERT INTO `book_category` VALUES (1,1),(2,2),(2,3),(3,4),(4,5),(5,2),(8,8);
/*!40000 ALTER TABLE `book_category` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `borrow_record`
--

DROP TABLE IF EXISTS `borrow_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `borrow_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `archive_reason` varchar(255) DEFAULT NULL,
  `archived_at` datetime(6) DEFAULT NULL,
  `archived_by` varchar(255) DEFAULT NULL,
  `borrow_date` date DEFAULT NULL,
  `due_date` date DEFAULT NULL,
  `is_archived` bit(1) NOT NULL,
  `late_fee` decimal(10,2) NOT NULL,
  `return_date` datetime(6) DEFAULT NULL,
  `status` enum('ACTIVE','OVERDUE','RETURNED') DEFAULT NULL,
  `book_id` int NOT NULL,
  `member_id` int NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKblllbxv8r2dt3j07c3hgdruqi` (`book_id`),
  KEY `FKspnmeha37ht996w5a4ng0uvuw` (`member_id`),
  CONSTRAINT `FKblllbxv8r2dt3j07c3hgdruqi` FOREIGN KEY (`book_id`) REFERENCES `book` (`id`),
  CONSTRAINT `FKspnmeha37ht996w5a4ng0uvuw` FOREIGN KEY (`member_id`) REFERENCES `member` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `borrow_record`
--

LOCK TABLES `borrow_record` WRITE;
/*!40000 ALTER TABLE `borrow_record` DISABLE KEYS */;
INSERT INTO `borrow_record` VALUES (1,NULL,NULL,NULL,'2026-02-26','2026-03-05',_binary '\0',20.00,NULL,'OVERDUE',1,1),(2,NULL,NULL,NULL,'2026-02-26','2026-03-05',_binary '\0',20.00,NULL,'OVERDUE',2,1),(3,NULL,NULL,NULL,'2026-03-05','2026-03-12',_binary '\0',13.00,NULL,'OVERDUE',3,6),(4,NULL,NULL,NULL,'2026-03-05','2026-03-12',_binary '\0',0.00,'2026-03-06 19:27:57.172118','RETURNED',5,5),(5,'no need','2026-03-06 19:35:59.030412','tillu','2026-03-05','2026-03-12',_binary '',0.00,'2026-03-06 19:19:56.455780','RETURNED',8,5),(6,NULL,NULL,NULL,'2026-03-06','2026-03-20',_binary '\0',5.00,NULL,'OVERDUE',3,6),(7,NULL,NULL,NULL,'2026-03-06','2026-03-20',_binary '\0',5.00,NULL,'OVERDUE',4,6),(8,NULL,NULL,NULL,'2026-03-06','2026-04-05',_binary '\0',0.00,NULL,'ACTIVE',4,4),(9,NULL,NULL,NULL,'2026-03-06','2026-03-13',_binary '\0',12.00,NULL,'OVERDUE',1,1),(10,NULL,NULL,NULL,'2026-03-18','2026-03-25',_binary '\0',0.00,NULL,'ACTIVE',1,17),(11,NULL,NULL,NULL,'2026-03-18','2026-03-25',_binary '\0',0.00,NULL,'ACTIVE',2,17),(12,NULL,NULL,NULL,'2026-03-18','2026-03-25',_binary '\0',0.00,NULL,'ACTIVE',1,18),(13,NULL,NULL,NULL,'2026-03-19','2026-03-26',_binary '\0',0.00,'2026-03-19 13:47:13.428754','RETURNED',3,18),(14,NULL,NULL,NULL,'2026-03-19','2026-03-26',_binary '\0',0.00,NULL,'ACTIVE',3,20),(15,NULL,NULL,NULL,'2026-03-19','2026-03-26',_binary '\0',0.00,NULL,'ACTIVE',5,20);
/*!40000 ALTER TABLE `borrow_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `category`
--

DROP TABLE IF EXISTS `category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `category` (
  `id` int NOT NULL AUTO_INCREMENT,
  `description` varchar(255) DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `category`
--

LOCK TABLES `category` WRITE;
/*!40000 ALTER TABLE `category` DISABLE KEYS */;
INSERT INTO `category` VALUES (1,'Fiction related','Fiction'),(2,'Romance related','Romance'),(3,'Historical related','Historical'),(4,'Software development books','Programming'),(5,'Fiction books','Fiction'),(6,'Science and technology','Science'),(8,'Humor related','Humor');
/*!40000 ALTER TABLE `category` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `member`
--

DROP TABLE IF EXISTS `member`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `member` (
  `id` int NOT NULL AUTO_INCREMENT,
  `email` varchar(255) NOT NULL,
  `membership_date` date DEFAULT NULL,
  `membership_type` enum('BASIC','PREMIUM','STANDARD') NOT NULL,
  `name` varchar(255) NOT NULL,
  `phone` varchar(255) DEFAULT NULL,
  `user_id` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKmbmcqelty0fbrvxp1q58dn57t` (`email`),
  UNIQUE KEY `UKa9bw6sk85ykh4bacjpu0ju5f6` (`user_id`),
  CONSTRAINT `FKe6yo8tn29so0kdd1mw4qk8tgh` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `member`
--

LOCK TABLES `member` WRITE;
/*!40000 ALTER TABLE `member` DISABLE KEYS */;
INSERT INTO `member` VALUES (1,'admin@gmail.com','2026-02-26','BASIC','admin',NULL,1),(4,'testing@gmail.com','2026-03-01','PREMIUM','test12','1253456789',8),(5,'Kiran@gmail.com','2026-03-02','PREMIUM','Kiran','7800898930',9),(6,'lib@gmail.com','2026-03-02','STANDARD','librarian',NULL,10),(7,'Test123@gmail.com','2026-03-12','BASIC','Test123',NULL,11),(8,'sai@example.com','2026-03-16','BASIC','saisk',NULL,12),(17,'saikrishna@gmail.com','2026-03-18','BASIC','skdev1','2905479430',21),(18,'198r1a0507@gmail.com','2026-03-18','BASIC','skdev','2905479430',22),(19,'user@example.com','2026-03-19','BASIC','string','2598600518',23),(20,'vidhyadhar80746@gmail.com','2026-03-19','BASIC','vidhyadhar','3266486147',24),(21,'Timin@gmail.com','2026-06-18','BASIC','Timin','6793793930',25);
/*!40000 ALTER TABLE `member` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` int NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `email` varchar(255) NOT NULL,
  `enabled` bit(1) NOT NULL,
  `password` varchar(255) NOT NULL,
  `role` enum('ADMIN','LIBRARIAN','MEMBER') NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `username` varchar(255) NOT NULL,
  `phone` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK6dotkott2kjsp8vw4d0m25fb7` (`email`),
  UNIQUE KEY `UKr43af9ap4edm43mmtq01oddj6` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=26 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,'2026-02-26 18:42:04.095335','admin@gmail.com',_binary '','$2a$12$NS0o2d4dCT9FUfp7CpkUfu0gr7T5AY3WW88G.jyTXV3A2ytTtjI8m','ADMIN','2026-02-26 18:42:04.095335','admin',NULL),(3,'2026-02-26 19:16:12.645009','skDB@gmail.com',_binary '','$2a$12$H7c1pn/XBgafx2fO6l8Htux1Vop8PexOcrap9PT27x.EgA0jlSCAK','LIBRARIAN','2026-02-26 19:16:12.645009','skDB',NULL),(8,'2026-03-01 20:52:22.784272','testing@gmail.com',_binary '','$2a$06$bp4NujY8dCp5dE.bMXnEpeTqzVuFKduQwiYx4E91D/05Fp8qAdrf2','MEMBER','2026-03-01 20:52:22.784272','test12',NULL),(9,'2026-03-02 17:50:34.673063','Kiran@gmail.com',_binary '','$2a$06$IY.VbhvSCi8oFKw/mYGZjuqAdfEzDz1H3yldDbqCBktJo85saR/6K','MEMBER','2026-03-02 17:50:34.673063','Kiran',NULL),(10,'2026-03-02 18:19:52.541524','lib@gmail.com',_binary '','$2a$06$7bNYZ2tDS/GMa8K71eikYONX0gHO0szqGN0Pw0d9.yryhF9KFNABW','LIBRARIAN','2026-03-02 18:19:52.541524','librarian',NULL),(11,'2026-03-12 15:39:17.379237','Test123@gmail.com',_binary '','$2a$06$fSZBoLq56Hs8UeAZ7TKWLOUbcM3SZV7H3F/GxJVT0fhQwoGWu1cLu','MEMBER','2026-03-12 15:39:17.379237','Test123',NULL),(12,'2026-03-16 18:12:17.066839','sai@example.com',_binary '','$2a$06$9bKXYeZ.PDBU7mRVSYS2uuCA9WBL7ePZqtLMfjufpfaZirVHWc90K','MEMBER','2026-03-16 18:12:17.066839','saisk',NULL),(21,'2026-03-18 19:28:44.105317','saikrishna@gmail.com',_binary '','$2a$06$yGezQUSA1q9354vWPqVSbeLieYY4SaqtLR2IgubpnEnG34x9H1eIW','MEMBER','2026-03-18 19:28:44.105317','skdev1','2905479430'),(22,'2026-03-18 19:37:07.626797','198r1a0507@gmail.com',_binary '','$2a$06$WX3xhJ1LnCospTSoL/XafuHlP8z4eGx7ETHvH3HnLrlQeHqHsexeG','MEMBER','2026-03-18 19:37:07.639080','skdev','2905479430'),(23,'2026-03-19 13:24:38.201690','user@example.com',_binary '','$2a$06$m92JHwUzETu5P1Ifh5Jv9OVlC9kAYQxFaMK4Smy9oWC300sIuqj.i','MEMBER','2026-03-19 13:24:38.201690','string','2598600518'),(24,'2026-03-19 13:50:50.226702','vidhyadhar80746@gmail.com',_binary '','$2a$06$dvRdQjozyMXqf9a1xSPz5OI1YlGwTC0ITeYLwl1H/75VWaX0oGGvO','MEMBER','2026-03-19 13:50:50.258295','vidhyadhar','3266486147'),(25,'2026-06-18 22:20:11.333001','Timin@gmail.com',_binary '','$2a$10$duk3G0S3cmbN6ibqkbnvYeUVdUB3WpztGVGaOGPZZ8Az3OUeZT8xW','MEMBER','2026-06-18 22:20:11.333001','Timin','6793793930');
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-06-19 16:52:01
