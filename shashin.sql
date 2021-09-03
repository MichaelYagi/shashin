DROP TABLE IF EXISTS `hibernate_sequence`;
CREATE TABLE `hibernate_sequence` (
    `next_val` BIGINT(20) DEFAULT NULL
);

DROP TABLE IF EXISTS `metadata`;
CREATE TABLE `metadata` (
    `id` VARCHAR(36) NOT NULL DEFAULT '00000000-00000000-00000000-00000000',
    `time_zone` VARCHAR(20) DEFAULT NULL,
    `taken_at` DATETIME DEFAULT NULL,
    `year` INT(4) DEFAULT NULL,
    `month` INT(2) DEFAULT NULL,
    `day` INT(2) DEFAULT NULL,
    `path` VARCHAR(255) DEFAULT NULL,
    `file_name` VARCHAR(255) DEFAULT NULL,
    `thumbnail_path_small` VARCHAR(255) DEFAULT NULL,
    `thumbnail_url_small` VARCHAR (255) DEFAULT NULL,
    `thumbnail_path_centered` VARCHAR(255) DEFAULT NULL,
    `thumbnail_url_centered` VARCHAR (255) DEFAULT NULL,
    `thumbnail_url_original` VARCHAR (255) DEFAULT NULL,
    `map_marker_path` VARCHAR (255) DEFAULT NULL,
    `map_marker_url` VARCHAR (255) DEFAULT NULL,
    `video_url` VARCHAR (255) DEFAULT NULL,
    `type` VARCHAR(20) DEFAULT NULL,
    `lat` VARCHAR(20) DEFAULT NULL,
    `lng` VARCHAR(20) DEFAULT NULL,
    `place_name` VARCHAR(255) DEFAULT NULL,
    `camera` VARCHAR(255) DEFAULT NULL,
    `lens` VARCHAR(255) DEFAULT NULL,
    `quality` VARCHAR(20) DEFAULT NULL,
    `iso` INT(10) DEFAULT NULL,
    `exposure` VARCHAR(20) DEFAULT NULL,
    `f_number` REAL(10) DEFAULT NULL,
    `focal_length` REAL(10) DEFAULT NULL,
    `keywords` VARCHAR(500) DEFAULT NULL,
    `recognition_label_id` INT(10) DEFAULT NULL,
    `recognition_confidence` VARCHAR(36) NOT NULL DEFAULT '100.0', -- 100.0 to be labelled, -1.0 not a face
    `created_at` DATETIME DEFAULT NULL,
    `modified_at` DATETIME DEFAULT NULL,
    `last_accessed_at` DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`),
    FOREIGN KEY (`recognition_label_id`) REFERENCES recognitionlabel(`id`)
);

DROP TABLE IF EXISTS `recognitionlabel`;
CREATE TABLE `recognitionlabel` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
    `name` VARCHAR(50) NOT NULL,
    `created_at` DATETIME DEFAULT NULL,
    `modified_at` DATETIME DEFAULT NULL
);

DROP TABLE IF EXISTS `settings`;
CREATE TABLE `settings` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
    `recognition_confidence_threshold` VARCHAR(36) NOT NULL DEFAULT '0.6',
    `query_limit` INTEGER NOT NULL DEFAULT 20,
    `created_at` DATETIME DEFAULT NULL,
    `modified_at` DATETIME DEFAULT NULL
);
INSERT INTO `settings` (`recognition_confidence_threshold`,`query_limit`) VALUES('0.6',20);

DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
    `username` VARCHAR(50) NOT NULL,
    `password` VARCHAR(50) NOT NULL,
    `authority` VARCHAR(50) NOT NULL,
    `logged_in` BOOLEAN DEFAULT NULL CHECK (`logged_in` IN (0, 1)),
    `created_at` DATETIME DEFAULT NULL,
    `modified_at` DATETIME DEFAULT NULL
);

DROP TABLE IF EXISTS `mediadir`;
CREATE TABLE `mediadir` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
    `directory` VARCHAR(150) NOT NULL,
    `created_at` DATETIME DEFAULT NULL,
    `modified_at` DATETIME DEFAULT NULL
);

DROP TABLE IF EXISTS `album`;
CREATE TABLE `album` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
    `name` VARCHAR(100) NOT NULL UNIQUE,
    `cover_url` VARCHAR(255) DEFAULT NULL,
    `share_url` VARCHAR(255) DEFAULT NULL,
    `created_at` DATETIME DEFAULT NULL,
    `modified_at` DATETIME DEFAULT NULL
);

DROP TABLE IF EXISTS `useralbum`;
CREATE TABLE `useralbum` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
    `user_id` INTEGER,
    `album_id` INTEGER,
    `created_at` DATETIME DEFAULT NULL,
    `modified_at` DATETIME DEFAULT NULL,
    UNIQUE(`user_id`,`album_id`) ON CONFLICT IGNORE,
    FOREIGN KEY (`album_id`) REFERENCES album(`id`),
    FOREIGN KEY (`user_id`) REFERENCES user(`id`)
);

DROP TABLE IF EXISTS `albumphoto`;
CREATE TABLE `albumphoto` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
    `metadata_id` VARCHAR(36),
    `album_id` INTEGER,
    `created_at` DATETIME DEFAULT NULL,
    `modified_at` DATETIME DEFAULT NULL,
    UNIQUE(`metadata_id`,`album_id`) ON CONFLICT IGNORE,
    FOREIGN KEY (`album_id`) REFERENCES album(`id`),
    FOREIGN KEY (`metadata_id`) REFERENCES metadata(`id`)
);

DROP TABLE IF EXISTS `favorite`;
CREATE TABLE `favorite` (
     `id` INTEGER PRIMARY KEY AUTOINCREMENT,
     `user_id` INTEGER,
     `metadata_id` INTEGER,
     `created_at` DATETIME DEFAULT NULL,
     `modified_at` DATETIME DEFAULT NULL,
     UNIQUE(`metadata_id`,`user_id`) ON CONFLICT IGNORE,
     FOREIGN KEY (`metadata_id`) REFERENCES metadata(`id`),
     FOREIGN KEY (`user_id`) REFERENCES user(`id`)
);

DROP TABLE IF EXISTS `comment`;
CREATE TABLE `comment` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
    `comment` VARCHAR,
    `user_id` INT,
    `created_at` DATETIME DEFAULT NULL,
    `modified_at` DATETIME DEFAULT NULL
);

DROP TABLE IF EXISTS `albumcomment`;
CREATE TABLE `albumcomment` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
    `album_id` INT,
    `comment_id` INT,
    `created_at` DATETIME DEFAULT NULL,
    `modified_at` DATETIME DEFAULT NULL,
    UNIQUE(`comment_id`,`album_id`) ON CONFLICT IGNORE,
    FOREIGN KEY (`comment_id`) REFERENCES comment(`id`),
    FOREIGN KEY (`album_id`) REFERENCES album(`id`)
);

DROP TABLE IF EXISTS `albumphotocomment`;
CREATE TABLE `albumphotocomment` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
    `metadata_id` INT,
    `album_id` INT,
    `comment_id` INT,
    `created_at` DATETIME DEFAULT NULL,
    `modified_at` DATETIME DEFAULT NULL,
    UNIQUE(`comment_id`,`metadata_id`,`album_id`) ON CONFLICT IGNORE,
    FOREIGN KEY (`comment_id`) REFERENCES comment(`id`),
    FOREIGN KEY (`metadata_id`) REFERENCES metadata(`id`),
    FOREIGN KEY (`album_id`) REFERENCES album(`id`)
);

INSERT INTO `hibernate_sequence` VALUES (362);