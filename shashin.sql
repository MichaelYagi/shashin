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
    `username` char(50) NOT NULL,
    `password` char(50) NOT NULL,
    `loggedIn` int(1) NOT NULL,
    `createdAt` datetime DEFAULT NULL,
    `modifiedAt` datetime DEFAULT NULL
);
DROP TABLE IF EXISTS `authorities`;
CREATE TABLE `authorities` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
    `userId` INTEGER NOT NULL,
    `authority` VARCHAR(50) NOT NULL,
    FOREIGN KEY (`userId`) REFERENCES `user`(`id`)
);
INSERT INTO `hibernate_sequence` VALUES (362);