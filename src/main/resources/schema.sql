DROP TABLE IF EXISTS `hibernate_sequence`;
CREATE TABLE `hibernate_sequence` (
                                      `next_val` BIGINT(20) DEFAULT NULL
);

DROP TABLE IF EXISTS `metadata`;
CREATE TABLE `metadata` (
                            `id` VARCHAR(36) NOT NULL DEFAULT '00000000-00000000-00000000-00000000',
                            `title` VARCHAR(255) DEFAULT NULL,
                            `description` VARCHAR(500) DEFAULT NULL,
                            `freeFormString` VARCHAR(500) DEFAULT NULL,
                            `timeZone` VARCHAR(20) DEFAULT NULL,
                            `year` INT(4) DEFAULT NULL,
                            `month` INT(2) DEFAULT NULL,
                            `day` INT(2) DEFAULT NULL,
                            `time` VARCHAR(8) DEFAULT NULL,
                            `path` VARCHAR(255) DEFAULT NULL,
                            `fileName` VARCHAR(255) DEFAULT NULL,
                            `expectedExtension` VARCHAR(255) DEFAULT NULL,
                            `thumbnailPathExtraSmall` VARCHAR(255) DEFAULT NULL,
                            `thumbnailUrlExtraSmall` VARCHAR (255) DEFAULT NULL,
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
                            `folder` VARCHAR (255) DEFAULT NULL,
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
                            `fstopNumber` REAL(10) DEFAULT NULL,
                            `focalLength` REAL(10) DEFAULT NULL,
                            `compressionType` VARCHAR(255) DEFAULT NULL,
                            `hidden` BOOLEAN NOT NULL DEFAULT FALSE,
                            `addedAt` DATETIME DEFAULT NULL,
                            `takenAt` DATETIME DEFAULT NULL,
                            `createdAt` DATETIME DEFAULT NULL,
                            `modifiedAt` DATETIME DEFAULT NULL,
                            `lastAccessedAt` DATETIME DEFAULT NULL,
                            `lastAccessedBy` INTEGER DEFAULT NULL,
                            `uploadedBy` INTEGER DEFAULT NULL,
                            PRIMARY KEY (`id`)
);

DROP INDEX IF EXISTS `idx_metadata_datetime`;
CREATE INDEX `idx_metadata_datetime` ON metadata (`year` DESC, `month` DESC, `day` DESC, `time` DESC);
DROP INDEX IF EXISTS `idx_metadata_datetype`;
CREATE INDEX `idx_metadata_datetype` ON metadata (`year` DESC, `month` DESC, `day` DESC, `hidden`, `type`);
DROP INDEX IF EXISTS `idx_metadata_date`;
CREATE INDEX `idx_metadata_date` ON metadata (`year` DESC, `month` DESC, `day` DESC, `hidden`);

DROP TABLE IF EXISTS `folderdata`;
CREATE TABLE `folderdata` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT,
                            `folder` VARCHAR (255) NOT NULL UNIQUE,
                            `mid` VARCHAR(36) NOT NULL UNIQUE
);

DROP TABLE IF EXISTS `settings`;
CREATE TABLE `settings` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT,
                            `recognitionConfidenceThreshold` VARCHAR(36) NOT NULL DEFAULT '0.8',
                            `objectRecognitionConfidenceThreshold` VARCHAR(36) NOT NULL DEFAULT '0.45',
                            `trainingDataLimit` INTEGER NOT NULL DEFAULT 100,
                            `matchScanLimit` INTEGER NOT NULL DEFAULT 30,
                            `queryLimit` INTEGER NOT NULL DEFAULT 30,
                            `notificationLimit` INTEGER NOT NULL DEFAULT 20,
                            `searchHistoryLimit` INTEGER NOT NULL DEFAULT 10,
                            `port` VARCHAR(10) NOT NULL DEFAULT '6624',
                            `scanAutomatically` BOOLEAN DEFAULT FALSE,
                            `scheduledMatching` BOOLEAN DEFAULT FALSE,
                            `scheduledTime` VARCHAR(20) DEFAULT '2:00',
                            `objectDetection` BOOLEAN DEFAULT FALSE,
                            `facialDetection` BOOLEAN DEFAULT FALSE,
                            `compreFaceKey` VARCHAR(36),
                            `compreFaceServer` VARCHAR(150),
                            `uploadMediaDirectory` VARCHAR(255) DEFAULT NULL,
                            `sidecarSizeK` REAL DEFAULT 0,
                            `createdAt` DATETIME DEFAULT NULL,
                            `modifiedAt` DATETIME DEFAULT NULL
);
CREATE TRIGGER settings_no_insert
    BEFORE INSERT ON `settings`
    WHEN (SELECT COUNT(*) FROM `settings`) >= 1
BEGIN
    SELECT RAISE(FAIL, 'only one row allowed!');
END;

DROP TABLE IF EXISTS `keyword`;
CREATE TABLE `keyword` (
                           `id` INTEGER PRIMARY KEY AUTOINCREMENT,
                           `keyword` VARCHAR(100) NOT NULL UNIQUE,
                           `createdAt` DATETIME DEFAULT NULL,
                           `modifiedAt` DATETIME DEFAULT NULL,
                           CHECK(`keyword` <> '')
);

DROP TABLE IF EXISTS `keywordphoto`;
CREATE TABLE `keywordphoto` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT,
                                `metadataId` VARCHAR(36),
                                `keywordId` INTEGER,
                                `createdAt` DATETIME DEFAULT NULL,
                                `modifiedAt` DATETIME DEFAULT NULL,
                                UNIQUE(`metadataId`,`keywordId`) ON CONFLICT IGNORE,
                                FOREIGN KEY (`keywordId`) REFERENCES keyword(`id`),
                                FOREIGN KEY (`metadataId`) REFERENCES metadata(`id`)
);

DROP TABLE IF EXISTS `recognitionlabel`;
CREATE TABLE `recognitionlabel` (
                                    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
                                    `name` VARCHAR(50) NOT NULL,
                                    `coverUrl` VARCHAR(255) DEFAULT NULL,
                                    `createdAt` DATETIME DEFAULT NULL,
                                    `modifiedAt` DATETIME DEFAULT NULL,
                                    CHECK(`name` <> '')
);

DROP TABLE IF EXISTS `recognitionlabelphoto`;
CREATE TABLE `recognitionlabelphoto` (
                                         `id` INTEGER PRIMARY KEY AUTOINCREMENT,
                                         `recognitionLabelId` INTEGER,
                                         `metadataId` VARCHAR(36),
                                         `compreFaceImageId` VARCHAR(36),
                                         `confidence` VARCHAR(36) NOT NULL DEFAULT '99.0', -- 99.0 to be labelled, -1.0 not a face
                                         `autoTagged` BOOLEAN NOT NULL DEFAULT FALSE,
                                         UNIQUE(`recognitionLabelId`,`metadataId`) ON CONFLICT IGNORE,
                                         FOREIGN KEY (`recognitionLabelId`) REFERENCES recognitionlabel(`id`),
                                         FOREIGN KEY (`metadataId`) REFERENCES metadata(`id`)
);

DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT,
                        `profile` VARCHAR(255) DEFAULT NULL,
                        `username` VARCHAR(64) NOT NULL,
                        `password` VARCHAR(64) NOT NULL,
                        `authority` VARCHAR(50) NOT NULL,
                        `isAuthorized` BOOLEAN NOT NULL DEFAULT FALSE,
                        `notificationAlerts` BOOLEAN NOT NULL DEFAULT TRUE,
                        `showPlacename` BOOLEAN NOT NULL DEFAULT FALSE,
                        `darkMode` BOOLEAN NOT NULL DEFAULT FALSE,
                        `autoplayVideo` BOOLEAN NOT NULL DEFAULT FALSE,
                        `slideshowInterval` INTEGER NOT NULL DEFAULT 10,
                        `slideshowProgress` BOOLEAN NOT NULL DEFAULT FALSE,
                        `apikey` VARCHAR(36) NOT NULL DEFAULT '00000000-00000000-00000000-00000000',
                        `language` VARCHAR(10) NOT NULL DEFAULT 'en',
                        `createdAt` DATETIME DEFAULT NULL,
                        `modifiedAt` DATETIME DEFAULT NULL,
                        CHECK(`username` <> ''),
                        CHECK(`password` <> '')
);

DROP TABLE IF EXISTS `slideshowalbum`;
CREATE TABLE `slideshowalbum` (
                             `id` INTEGER PRIMARY KEY AUTOINCREMENT,
                             `userId` INTEGER,
                             `albums` VARCHAR(255) DEFAULT 'all',
                             UNIQUE(`userId`) ON CONFLICT IGNORE,
                             FOREIGN KEY (`userId`) REFERENCES user(`id`)
);

DROP TABLE IF EXISTS `useragent`;
CREATE TABLE `useragent` (
                             `id` INTEGER PRIMARY KEY AUTOINCREMENT,
                             `userId` INTEGER,
                             `deviceClass` VARCHAR(64) DEFAULT NULL,
                             `osClass` VARCHAR(64) DEFAULT NULL,
                             `osName` VARCHAR(64) DEFAULT NULL,
                             `osVersion` VARCHAR(64) DEFAULT NULL,
                             `agentName` VARCHAR(64) DEFAULT NULL,
                             `agentVersion` VARCHAR(64) DEFAULT NULL,
                             `createdAt` DATETIME DEFAULT NULL,
                             FOREIGN KEY (`userId`) REFERENCES user(`id`)
);

DROP TABLE IF EXISTS `mediadir`;
CREATE TABLE `mediadir` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT,
                            `directory` VARCHAR(150) NOT NULL,
                            `exclude` BOOLEAN DEFAULT FALSE,
                            `createdAt` DATETIME DEFAULT NULL,
                            `modifiedAt` DATETIME DEFAULT NULL,
                            CHECK(`directory` <> '')
);

DROP TABLE IF EXISTS `album`;
CREATE TABLE `album` (
                         `id` INTEGER PRIMARY KEY AUTOINCREMENT,
                         `name` VARCHAR(100) NOT NULL UNIQUE,
                         `coverUrl` VARCHAR(255) DEFAULT NULL,
                         `shareUrl` VARCHAR(255) DEFAULT NULL,
                         `createdAt` DATETIME DEFAULT NULL,
                         `modifiedAt` DATETIME DEFAULT NULL,
                         CHECK(`name` <> '')
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
                           `modifiedAt` DATETIME DEFAULT NULL,
                           FOREIGN KEY (`userId`) REFERENCES user(`id`)
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
                                `identifier` VARCHAR(36) DEFAULT NULL,
                                `imageUrl` VARCHAR,
                                `read` BOOLEAN DEFAULT 0,
                                `message` VARCHAR,
                                `createdAt` DATETIME DEFAULT NULL,
                                `modifiedAt` DATETIME DEFAULT NULL,
                                FOREIGN KEY (`userId`) REFERENCES user(`id`)
);

DROP TABLE IF EXISTS `searchhistory`;
CREATE TABLE `searchhistory` (
                                 `id` INTEGER PRIMARY KEY AUTOINCREMENT,
                                 `term` VARCHAR(150) NOT NULL,
                                 `userId` INT,
                                 `searchType` INT,
                                 `createdAt` DATETIME DEFAULT NULL,
                                 `modifiedAt` DATETIME DEFAULT NULL,
                                 FOREIGN KEY (`userId`) REFERENCES user(`id`)
);

INSERT INTO `hibernate_sequence` VALUES (362);

PRAGMA journal_mode = WAL;
PRAGMA synchronous = NORMAL;