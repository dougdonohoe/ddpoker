--
-- =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
-- DD Poker - Source Code
-- Copyright (c) 2003-2026 Doug Donohoe
-- 
-- This program is free software: you can redistribute it and/or modify
-- it under the terms of the GNU General Public License as published by
-- the Free Software Foundation, either version 3 of the License, or
-- (at your option) any later version.
-- 
-- This program is distributed in the hope that it will be useful,
-- but WITHOUT ANY WARRANTY; without even the implied warranty of
-- MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
-- GNU General Public License for more details.
-- 
-- For the full License text, please see the LICENSE.txt file
-- in the root directory of this project.
-- 
-- The "DD Poker" and "Donohoe Digital" names and logos, as well as any images, 
-- graphics, text, and documentation found in this repository (including but not
-- limited to written documentation, website content, and marketing materials) 
-- are licensed under the Creative Commons Attribution-NonCommercial-NoDerivatives 
-- 4.0 International License (CC BY-NC-ND 4.0). You may not use these assets 
-- without explicit written permission for any uses not covered by this License.
-- For the full License text, please see the LICENSE-CREATIVE-COMMONS.txt file
-- in the root directory of this project.
-- 
-- For inquiries regarding commercial licensing of this source code or 
-- the use of names, logos, images, text, or other assets, please contact 
-- doug [at] donohoe [dot] info.
-- =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
--
use DBNAME;

CREATE TABLE wan_profile (
    wpr_id INT UNSIGNED NOT NULL PRIMARY KEY AUTO_INCREMENT,
    wpr_name VARCHAR(32) NOT NULL,
    wpr_license_key VARCHAR(55) NOT NULL,
    wpr_email VARCHAR(255) NOT NULL,
    wpr_password VARCHAR(255) NOT NULL,
    wpr_is_activated BOOLEAN NOT NULL,
    wpr_is_retired BOOLEAN NOT NULL,
    wpr_create_date DATETIME NOT NULL,
    wpr_modify_date DATETIME NOT NULL,

    UNIQUE INDEX wpr_name (wpr_name),
	INDEX wpr_email (wpr_email)
) Engine = InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE wan_game (
    wgm_id INT UNSIGNED NOT NULL PRIMARY KEY AUTO_INCREMENT,
    wgm_license_key VARCHAR(55) NOT NULL,
    wgm_url VARCHAR(64) NOT NULL,
    wgm_host_player VARCHAR(64) NOT NULL,
    wgm_start_date DATETIME NULL,
    wgm_end_date DATETIME NULL,
    wgm_create_date DATETIME NOT NULL,
    wgm_modify_date DATETIME NOT NULL,
	wgm_mode TINYINT NOT NULL,
    wgm_tournament_data TEXT NOT NULL,

    UNIQUE INDEX wgm_license_key (wgm_license_key, wgm_url),
    INDEX wgm_host_player (wgm_host_player),
    INDEX wgm_modify_date (wgm_modify_date),
    INDEX wgm_end_date (wgm_end_date),
	INDEX wgm_create_date_mode (wgm_create_date, wgm_mode),
	INDEX wgm_end_date_mode (wgm_end_date, wgm_mode),
    INDEX wgm_mode (wgm_mode)
) Engine = InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE wan_history (
    whi_id INT UNSIGNED NOT NULL PRIMARY KEY AUTO_INCREMENT,
    whi_game_id INT UNSIGNED NOT NULL,
	whi_tournament_name VARCHAR(255) NOT NULL,
	whi_num_players INT UNSIGNED NOT NULL,
	whi_is_ended BOOL NOT NULL,
    whi_profile_id INT UNSIGNED NOT NULL,
    whi_player_name VARCHAR(32) NOT NULL,
    whi_player_type TINYINT NOT NULL,
    whi_finish_place SMALLINT NOT NULL,
    whi_prize DECIMAL NOT NULL,
    whi_buy_in DECIMAL NOT NULL,
    whi_total_rebuy DECIMAL NOT NULL,
    whi_total_add_on DECIMAL NOT NULL,
	whi_rank_1 DECIMAL(10,3) NOT NULL,
    whi_disco DECIMAL(10,0) NOT NULL,
    whi_end_date DATETIME NOT NULL,

    FOREIGN KEY (whi_game_id) REFERENCES wan_game(wgm_id),
    FOREIGN KEY (whi_profile_id) REFERENCES wan_profile(wpr_id),
    INDEX whi_end_date (whi_end_date),
    INDEX whi_player_type (whi_player_type),
    INDEX whi_is_ended (whi_is_ended)
) Engine = InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE registration (
    reg_id INT UNSIGNED NOT NULL PRIMARY KEY AUTO_INCREMENT,
    reg_license_key VARCHAR(55) NOT NULL,
    reg_product_version VARCHAR(32) NOT NULL,
    reg_ip_address VARCHAR(16) NOT NULL,
    reg_host_name VARCHAR(255) NULL,
    reg_host_name_modified VARCHAR(255) NULL,
    reg_port SMALLINT UNSIGNED NULL,
    reg_server_time DATETIME NOT NULL,
    reg_java_version VARCHAR(32) NULL,
    reg_os VARCHAR(32) NULL,
    reg_type TINYINT NOT NULL,
    reg_is_duplicate BOOL NOT NULL,
    reg_is_ban_attempt BOOL NOT NULL,
    reg_name VARCHAR(100) NULL,
    reg_email VARCHAR(255) NULL,
    reg_address VARCHAR(255) NULL,
    reg_city VARCHAR(50) NULL,
    reg_state VARCHAR(50) NULL,
    reg_postal VARCHAR(50) NULL,
    reg_country VARCHAR(120) NULL,

    INDEX reg_address (reg_address(255)),
    INDEX reg_email (reg_email(255)),
    INDEX reg_host_name_modified (reg_host_name_modified(255)),
    INDEX reg_ip_address (reg_ip_address),
    INDEX reg_is_duplicate (reg_is_duplicate),
    INDEX reg_name (reg_name),
    INDEX reg_license_key (reg_license_key),
    INDEX reg_type (reg_type)
) Engine = InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE banned_key (
    ban_id INT UNSIGNED NOT NULL PRIMARY KEY AUTO_INCREMENT,
    ban_key VARCHAR(255) NOT NULL,
	ban_until DATE NOT NULL,
    ban_comment VARCHAR(128) NULL,
    ban_create_date DATETIME NOT NULL,

    UNIQUE INDEX ban_key (ban_key)
) Engine = InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE upgraded_key (
    upg_id INT UNSIGNED NOT NULL PRIMARY KEY AUTO_INCREMENT,
    upg_license_key VARCHAR(55) NOT NULL,
    upg_count SMALLINT UNSIGNED NOT NULL,
    upg_create_date DATETIME NOT NULL,
    upg_modify_date DATETIME NOT NULL,

    UNIQUE INDEX upg_license_key (upg_license_key)
) Engine = InnoDB DEFAULT CHARSET=utf8;
