/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026 Doug Donohoe
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * For the full License text, please see the LICENSE.txt file
 * in the root directory of this project.
 *
 * The "DD Poker" and "Donohoe Digital" names and logos, as well as any images,
 * graphics, text, and documentation found in this repository (including but not
 * limited to written documentation, website content, and marketing materials)
 * are licensed under the Creative Commons Attribution-NonCommercial-NoDerivatives
 * 4.0 International License (CC BY-NC-ND 4.0). You may not use these assets
 * without explicit written permission for any uses not covered by this License.
 * For the full License text, please see the LICENSE-CREATIVE-COMMONS.txt file
 * in the root directory of this project.
 *
 * For inquiries regarding commercial licensing of this source code or
 * the use of names, logos, images, text, or other assets, please contact
 * doug [at] donohoe [dot] info.
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.proto.tests;

import java.sql.*;

/**
 * Class to demonstrate basic JDBC connectivity
 */
public class DatabaseExample {

    public DatabaseExample() {
    }

    public static void main(String[] args) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try {
                Connection conn = DriverManager.getConnection(
                        "jdbc:mysql://127.0.0.1/pokertest?useSSL=false", "pokertest", "p0k3rdb!");
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * from wan_profile");

                SQLWarning warn = rs.getWarnings();
                while (warn != null) {
                    System.out.println("Warning: " + warn.getMessage());
                    warn = warn.getNextWarning();
                }
                System.out.println("Results:");
                int nCnt = 1;
                while (rs.next()) {
                    System.out.print("Row " + nCnt++);
                    System.out.print(" - id: " + rs.getInt("object_id"));
                    System.out.println(", title: " + rs.getString("object_title"));
                }
                rs.close();
                stmt.close();
                conn.close();
            } catch (SQLException se) {
                while (se != null) {
                    System.out.println("SqlException: " + se.getMessage());
                    se.printStackTrace(System.out);
                    se = se.getNextException();
                }
            }
        } catch (ClassNotFoundException e) {
            System.out.println("ClassNotFound: " + e.getMessage());
        }
    }
}
