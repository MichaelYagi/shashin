DROP TABLE IF EXISTS `hibernate_sequence`;
CREATE TABLE `hibernate_sequence` (
    `next_val` BIGINT(20) DEFAULT NULL
);

DROP TABLE IF EXISTS `metadata`;
CREATE TABLE `metadata` (
    `id` VARCHAR(36) NOT NULL DEFAULT '00000000-00000000-00000000-00000000',
    `title` VARCHAR(255) DEFAULT NULL,
    `timeZone` VARCHAR(20) DEFAULT NULL,
    `year` INT(4) DEFAULT NULL,
    `month` INT(2) DEFAULT NULL,
    `day` INT(2) DEFAULT NULL,
    `time` VARCHAR(8) DEFAULT NULL,
    `path` VARCHAR(255) DEFAULT NULL,
    `fileName` VARCHAR(255) DEFAULT NULL,
    `thumbnailPathSmall` VARCHAR(255) DEFAULT NULL,
    `thumbnailUrlSmall` VARCHAR (255) DEFAULT NULL,
    `thumbnailPathCentered` VARCHAR(255) DEFAULT NULL,
    `thumbnailUrlCentered` VARCHAR (255) DEFAULT NULL,
    `thumbnailUrlOriginal` VARCHAR (255) DEFAULT NULL,
    `thumbnailSmallWidth` INTEGER DEFAULT NULL,
    `thumbnailSmallHeight` INTEGER DEFAULT NULL,
    `originalImageWidth` INTEGER DEFAULT NULL,
    `originalImageHeight` INTEGER DEFAULT NULL,
    `mapMarkerPath` VARCHAR (255) DEFAULT NULL,
    `mapMarkerUrl` VARCHAR (255) DEFAULT NULL,
    `videoUrl` VARCHAR (255) DEFAULT NULL,
    `duration` VARCHAR (8) DEFAULT NULL,
    `type` VARCHAR(20) DEFAULT NULL,
    `lat` VARCHAR(20) DEFAULT NULL,
    `lng` VARCHAR(20) DEFAULT NULL,
    `placeName` VARCHAR(255) DEFAULT NULL,
    `camera` VARCHAR(255) DEFAULT NULL,
    `lens` VARCHAR(255) DEFAULT NULL,
    `quality` VARCHAR(20) DEFAULT NULL,
    `iso` INT(10) DEFAULT NULL,
    `exposure` VARCHAR(20) DEFAULT NULL,
    `fNumber` REAL(10) DEFAULT NULL,
    `focalLength` REAL(10) DEFAULT NULL,
    `compressionType` VARCHAR(255) DEFAULT NULL,
    `keywords` VARCHAR(500) DEFAULT NULL,
    `hidden` BOOLEAN NOT NULL DEFAULT FALSE,
    `addedAt` DATETIME DEFAULT NULL,
    `takenAt` DATETIME DEFAULT NULL,
    `createdAt` DATETIME DEFAULT NULL,
    `modifiedAt` DATETIME DEFAULT NULL,
    `lastAccessedAt` DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`)
);

DROP TABLE IF EXISTS `recognitionlabel`;
CREATE TABLE `recognitionlabel` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
    `name` VARCHAR(50) NOT NULL,
    `createdAt` DATETIME DEFAULT NULL,
    `modifiedAt` DATETIME DEFAULT NULL
);

DROP TABLE IF EXISTS `recognitionlabelphoto`;
CREATE TABLE `recognitionlabelphoto` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
    `recognitionLabelId` INTEGER,
    `metadataId` VARCHAR(36),
    `confidence` VARCHAR(36) NOT NULL DEFAULT '99.0', -- 99.0 to be labelled, -1.0 not a face
    `autoTagged` BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE(`recognitionLabelId`,`metadataId`) ON CONFLICT IGNORE,
    FOREIGN KEY (`recognitionLabelId`) REFERENCES recognitionlabel(`id`),
    FOREIGN KEY (`metadataId`) REFERENCES metadata(`id`)
);

DROP TABLE IF EXISTS `settings`;
CREATE TABLE `settings` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
    `recognitionConfidenceThreshold` VARCHAR(36) NOT NULL DEFAULT '0.6',
    `traininDataLimit` INTEGER NOT NULL DEFAULT 100,
    `matchScanLimit` INTEGER NOT NULL DEFAULT 50,
    `queryLimit` INTEGER NOT NULL DEFAULT 20,
    `notificationLimit` INTEGER NOT NULL DEFAULT 20,
    `port` VARCHAR(10) NOT NULL DEFAULT '6624',
    `createdAt` DATETIME DEFAULT NULL,
    `modifiedAt` DATETIME DEFAULT NULL
);
-- INSERT INTO settings
--     (recognition_confidence_threshold,training_data_limit,match_scan_limit,query_limit,notification_limit,port)
--     VALUES ('0.6',100,10,20,20,'6624');

DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
    `username` VARCHAR(50) NOT NULL,
    `password` VARCHAR(50) NOT NULL,
    `authority` VARCHAR(50) NOT NULL,
    `isAllowed` BOOLEAN NOT NULL DEFAULT FALSE,
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
    `shareUrl` VARCHAR(255) DEFAULT NULL,
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
     `metadataId` VARCHAR(36),
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
    `userId` INT,
    `createdAt` DATETIME DEFAULT NULL,
    `modifiedAt` DATETIME DEFAULT NULL
);

DROP TABLE IF EXISTS `albumcomment`;
CREATE TABLE `albumcomment` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
    `albumId` INT,
    `commentId` INT,
    `createdAt` DATETIME DEFAULT NULL,
    `modifiedAt` DATETIME DEFAULT NULL,
    UNIQUE(`commentId`,`albumId`) ON CONFLICT IGNORE,
    FOREIGN KEY (`commentId`) REFERENCES comment(`id`),
    FOREIGN KEY (`albumId`) REFERENCES album(`id`)
);

DROP TABLE IF EXISTS `albumphotocomment`;
CREATE TABLE `albumphotocomment` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
    `metadataId` VARCHAR(36),
    `albumId` INT,
    `commentId` INT,
    `createdAt` DATETIME DEFAULT NULL,
    `modifiedAt` DATETIME DEFAULT NULL,
    UNIQUE(`commentId`,`metadataId`,`albumId`) ON CONFLICT IGNORE,
    FOREIGN KEY (`commentId`) REFERENCES comment(`id`),
    FOREIGN KEY (`metadataId`) REFERENCES metadata(`id`),
    FOREIGN KEY (`albumId`) REFERENCES album(`id`)
);

DROP TABLE IF EXISTS `notification`;
CREATE TABLE `notification` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
    `userId` INT,
    `albumId` INT DEFAULT NULL,
    `metadataId` VARCHAR(36) DEFAULT NULL,
    `commentId` INT DEFAULT NULL,
    `favoriteId` INT DEFAULT NULL,
    `read` BOOLEAN DEFAULT FALSE,
    `message` VARCHAR,
    `createdAt` DATETIME DEFAULT NULL,
    `modifiedAt` DATETIME DEFAULT NULL,
    FOREIGN KEY (`userId`) REFERENCES user(`id`)
);

INSERT INTO `hibernate_sequence` VALUES (362);