DROP TABLE IF EXISTS `hibernate_sequence`;
CREATE TABLE `hibernate_sequence` (
    `next_val` bigint(20) DEFAULT NULL
);

DROP TABLE IF EXISTS `metadata`;
CREATE TABLE `metadata` (
    `id` varchar(36) NOT NULL DEFAULT '00000000-00000000-00000000-00000000',
    `timeZone` varchar(20) DEFAULT NULL,
    `takenAt` datetime DEFAULT NULL,
    `year` int(4) DEFAULT NULL,
    `month` int(2) DEFAULT NULL,
    `day` int(2) DEFAULT NULL,
    `path` varchar(255) DEFAULT NULL,
    `fileName` varchar(255) DEFAULT NULL,
    `thumbnailPathSmall` varchar(255) DEFAULT NULL,
    `thumbnailUrlSmall` varchar (255) DEFAULT NULL,
    `thumbnailPathCentered` varchar(255) DEFAULT NULL,
    `thumbnailUrlCentered` varchar (255) DEFAULT NULL,
    `thumbnailPathOriginal` varchar(255) DEFAULT NULL,
    `thumbnailUrlOriginal` varchar (255) DEFAULT NULL,
    `videoUrl` varchar (255) DEFAULT NULL,
    `type` varchar(20) DEFAULT NULL,
    `lat` varchar(20) DEFAULT NULL,
    `lng` varchar(20) DEFAULT NULL,
    `camera` varchar(255) DEFAULT NULL,
    `lens` varchar(255) DEFAULT NULL,
    `quality` varchar(20) DEFAULT NULL,
    `iso` int(10) DEFAULT NULL,
    `exposure` varchar(20) DEFAULT NULL,
    `fNumber` real(10) DEFAULT NULL,
    `focalLength` real(10) DEFAULT NULL,
    `keywords` varchar(500) DEFAULT NULL,
    `createdAt` datetime DEFAULT NULL,
    `modifiedAt` datetime DEFAULT NULL,
    `lastAccessedAt` datetime DEFAULT NULL,
    PRIMARY KEY (`id`)
);

DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
    `username` VARCHAR(50) NOT NULL,
    `password` VARCHAR(50) NOT NULL,
    `authority` VARCHAR(50) NOT NULL,
    `loggedIn` BOOLEAN DEFAULT NULL CHECK (`loggedIn` IN (0, 1)),
    `createdAt` DATETIME DEFAULT NULL,
    `modifiedAt` DATETIME DEFAULT NULL
);

DROP TABLE IF EXISTS `mediadir`;
CREATE TABLE `mediadir` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
    `directory` VARCHAR(150) NOT NULL,
    `createdAt` DATETIME DEFAULT NULL,
    `modifiedAt` DATETIME DEFAULT NULL
);

DROP TABLE IF EXISTS `album`;
CREATE TABLE `album` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
    `name` VARCHAR(100) NOT NULL UNIQUE,
    `coverUrl` VARCHAR(255) DEFAULT NULL,
    `createdAt` DATETIME DEFAULT NULL,
    `modifiedAt` DATETIME DEFAULT NULL
);

DROP TABLE IF EXISTS `useralbum`;
CREATE TABLE `useralbum` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
    `userId` INTEGER,
    `albumId` INTEGER,
    `createdAt` DATETIME DEFAULT NULL,
    `modifiedAt` DATETIME DEFAULT NULL,
    UNIQUE(`userId`,`albumId`) ON CONFLICT IGNORE,
    FOREIGN KEY (`albumId`) REFERENCES album(`id`),
    FOREIGN KEY (`userId`) REFERENCES user(`id`)
);

DROP TABLE IF EXISTS `albumphoto`;
CREATE TABLE `albumphoto` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
    `metadataId` VARCHAR(36),
    `albumId` INTEGER,
    `createdAt` DATETIME DEFAULT NULL,
    `modifiedAt` DATETIME DEFAULT NULL,
    UNIQUE(`metadataId`,`albumId`) ON CONFLICT IGNORE,
    FOREIGN KEY (`albumId`) REFERENCES album(`id`),
    FOREIGN KEY (`metadataId`) REFERENCES metadata(`id`)
);

DROP TABLE IF EXISTS `favorite`;
CREATE TABLE `favorite` (
     `id` INTEGER PRIMARY KEY AUTOINCREMENT,
     `userId` INTEGER,
     `metadataId` INTEGER,
     `createdAt` DATETIME DEFAULT NULL,
     `modifiedAt` DATETIME DEFAULT NULL,
     UNIQUE(`metadataId`,`userId`) ON CONFLICT IGNORE,
     FOREIGN KEY (`metadataId`) REFERENCES metadata(`id`),
     FOREIGN KEY (`userId`) REFERENCES user(`id`)
);

DROP TABLE IF EXISTS `comment`;
CREATE TABLE `comment` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
    `comment` VARCHAR,
    `createdAt` DATETIME DEFAULT NULL,
    `modifiedAt` DATETIME DEFAULT NULL
);

DROP TABLE IF EXISTS `albumcomment`;
CREATE TABLE `albumcomment` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
    `albumId` INT,
    `userId` INT,
    `commentId` INT,
    `createdAt` DATETIME DEFAULT NULL,
    `modifiedAt` DATETIME DEFAULT NULL,
    UNIQUE(`commentId`,`userId`,`albumId`) ON CONFLICT IGNORE,
    FOREIGN KEY (`commentId`) REFERENCES comment(`id`),
    FOREIGN KEY (`userId`) REFERENCES user(`id`),
    FOREIGN KEY (`albumId`) REFERENCES album(`id`)
);

INSERT INTO `hibernate_sequence` VALUES (362);