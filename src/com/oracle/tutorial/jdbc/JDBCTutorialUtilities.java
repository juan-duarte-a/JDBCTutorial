/*
 * Copyright (c) 1995, 2011, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *     - Redistributions of source code must retain the above copyright
 *         notice, this list of conditions and the following disclaimer.
 *
 *     - Redistributions in binary form must reproduce the above copyright
 *         notice, this list of conditions and the following disclaimer in the
 *         documentation and/or other materials provided with the distribution.
 *
 *     - Neither the name of Oracle or the names of its
 *         contributors may be used to endorse or promote products derived
 *         from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.    IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.oracle.tutorial.jdbc;

import com.mysql.cj.jdbc.MysqlConnectionPoolDataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.*;
import java.io.*;
import java.sql.BatchUpdateException;
import java.sql.DatabaseMetaData;
import java.sql.RowIdLifetime;
import java.sql.SQLWarning;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.PooledConnection;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.mariadb.jdbc.MariaDbPoolDataSource;
import org.w3c.dom.Document;

public class JDBCTutorialUtilities {

    public String dbms;
    public String jarFile;
    public String dbName;
    public String userName;
    public String password;
    public String urlString;

    private String driver;
    private String serverName;
    private int portNumber;
    private Properties prop;
    
    private MariaDbPoolDataSource mariaDbPoolDataSource;
    private MysqlConnectionPoolDataSource mysqlConnectionPoolDataSource;

    public static void initializeTables(Connection conn, String dbNameArg, String dbmsArg)
            throws SQLException {
        SuppliersTable mySuppliersTable
                = new SuppliersTable(conn, dbNameArg, dbmsArg);
        CoffeesTable myCoffeeTable
                = new CoffeesTable(conn, dbNameArg, dbmsArg);
        RSSFeedsTable myRSSFeedsTable
                = new RSSFeedsTable(conn, dbNameArg, dbmsArg);
        ProductInformationTable myPIT
                = new ProductInformationTable(conn, dbNameArg, dbmsArg);

        System.out.println("\nDropping COFFEE_DESCRIPTIONS, COF_INVENTORY, and MERCH_INVENTORY tables");
        dropOtherTables(conn);

        System.out.println("\nDropping exisiting PRODUCT_INFORMATION, COFFEES and SUPPLIERS tables");
        myPIT.dropTable();
        myRSSFeedsTable.dropTable();
        myCoffeeTable.dropTable();
        mySuppliersTable.dropTable();
        
        System.out.println("\nCreating and populating SUPPLIERS table...");

        System.out.println("\nCreating SUPPLIERS table");
        mySuppliersTable.createTable();
        System.out.println("\nPopulating SUPPLIERS table");
        mySuppliersTable.populateTable();

        System.out.println("\nCreating and populating COFFEES table...");

        System.out.println("\nCreating COFFEES table");
        myCoffeeTable.createTable();
        System.out.println("\nPopulating COFFEES table");
        myCoffeeTable.populateTable();

        System.out.println("\nCreating RSS_FEEDS table...");
        myRSSFeedsTable.createTable();

        System.out.println("\nCreating PRODUCT_INFORMATION table...");
        myPIT.createTable();
        
        System.out.println("\nCreating COFFEE_DESCRIPTIONS, COF_INVENTORY, and MERCH_INVENTORY tables...");
        createOtherTables(conn);
    }
    
    public static void createOtherTables(Connection conn) {
        String createCoffeesDescriptionsTableSQL = 
                "create table COFFEE_DESCRIPTIONS (" 
                + "COF_NAME varchar(32) NOT NULL, " 
                + "COF_DESC blob NOT NULL, " 
                + "PRIMARY KEY (COF_NAME), " 
                + "FOREIGN KEY (COF_NAME) REFERENCES COFFEES (COF_NAME))";
        
        String createCofInventoryTableSQL =
                "create table COF_INVENTORY ("
                + "WAREHOUSE_ID integer NOT NULL, "
                + "COF_NAME varchar(32) NOT NULL, "
                + "SUP_ID int NOT NULL, "
                + "QUAN int NOT NULL, "
                + "DATE_VAL timestamp, "
                + "FOREIGN KEY (COF_NAME) REFERENCES COFFEES (COF_NAME), "
                + "FOREIGN KEY (SUP_ID) REFERENCES SUPPLIERS (SUP_ID))";
        
        String createMerchInventoryTableSQL =
                "create table MERCH_INVENTORY ("
                + "ITEM_ID integer NOT NULL, "
                + "ITEM_NAME varchar(20), "
                + "SUP_ID int, QUAN int, "
                + "DATE_VAL timestamp, "
                + "PRIMARY KEY (ITEM_ID), "
                + "FOREIGN KEY (SUP_ID) REFERENCES SUPPLIERS (SUP_ID))";
        
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(createCoffeesDescriptionsTableSQL);
            stmt.executeUpdate(createCofInventoryTableSQL);
            stmt.executeUpdate(createMerchInventoryTableSQL);
        } catch (SQLException e) {
            JDBCTutorialUtilities.printSQLException(e);
        }
    }
    
    public static void dropOtherTables(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            stmt.addBatch("DROP TABLE IF EXISTS MERCH_INVENTORY");
            stmt.addBatch("DROP TABLE IF EXISTS COF_INVENTORY");
            stmt.addBatch("DROP TABLE IF EXISTS COFFEE_DESCRIPTIONS");
            stmt.executeBatch();
        } catch (SQLException e) {
            JDBCTutorialUtilities.printSQLException(e);
        }
    }

    public static void rowIdLifetime(Connection conn) throws SQLException {
        DatabaseMetaData dbMetaData = conn.getMetaData();
        RowIdLifetime lifetime = dbMetaData.getRowIdLifetime();
        switch (lifetime) {
            case ROWID_UNSUPPORTED:
                System.out.println("ROWID type not supported");
                break;
            case ROWID_VALID_FOREVER:
                System.out.println("ROWID has unlimited lifetime");
                break;
            case ROWID_VALID_OTHER:
                System.out.println("ROWID has indeterminate lifetime");
                break;
            case ROWID_VALID_SESSION:
                System.out.println("ROWID type has lifetime that is valid for at least the containing session");
                break;
            case ROWID_VALID_TRANSACTION:
                System.out.println("ROWID type has lifetime that is valid for at least the containing transaction");
        }
    }

    public static void cursorHoldabilitySupport(Connection conn) throws SQLException {
        DatabaseMetaData dbMetaData = conn.getMetaData();
        System.out.println("ResultSet.HOLD_CURSORS_OVER_COMMIT = "
                + ResultSet.HOLD_CURSORS_OVER_COMMIT);
        System.out.println("ResultSet.CLOSE_CURSORS_AT_COMMIT = "
                + ResultSet.CLOSE_CURSORS_AT_COMMIT);
        System.out.println("Default cursor holdability: "
                + dbMetaData.getResultSetHoldability());
        System.out.println("Supports HOLD_CURSORS_OVER_COMMIT? "
                + dbMetaData.supportsResultSetHoldability(ResultSet.HOLD_CURSORS_OVER_COMMIT));
        System.out.println("Supports CLOSE_CURSORS_AT_COMMIT? "
                + dbMetaData.supportsResultSetHoldability(ResultSet.CLOSE_CURSORS_AT_COMMIT));
    }

    public JDBCTutorialUtilities(String propertiesFileName)
            throws FileNotFoundException, IOException, InvalidPropertiesFormatException {
        super();
        this.setProperties(propertiesFileName);
    }

    public static void getWarningsFromResultSet(ResultSet rs) throws SQLException {
        JDBCTutorialUtilities.printWarnings(rs.getWarnings());
    }

    public static void getWarningsFromStatement(Statement stmt) throws SQLException {
        JDBCTutorialUtilities.printWarnings(stmt.getWarnings());
    }

    public static void printWarnings(SQLWarning warning) throws SQLException {
        if (warning != null) {
            System.out.println("\n---Warning---\n");
            while (warning != null) {
                System.out.println("Message: " + warning.getMessage());
                System.out.println("SQLState: " + warning.getSQLState());
                System.out.print("Vendor error code: ");
                System.out.println(warning.getErrorCode());
                System.out.println("");
                warning = warning.getNextWarning();
            }
        }
    }

    public static boolean ignoreSQLException(String sqlState) {
        if (sqlState == null) {
            System.out.println("The SQL state is not defined!");
            return false;
        }
        // X0Y32: Jar file already exists in schema
        if (sqlState.equalsIgnoreCase("X0Y32")) {
            return true;
        }
        // 42Y55: Table already exists in schema
        return sqlState.equalsIgnoreCase("42Y55");
    }

    public static void printBatchUpdateException(BatchUpdateException b) {
        System.err.println("----BatchUpdateException----");
        System.err.println("SQLState:    " + b.getSQLState());
        System.err.println("Message:    " + b.getMessage());
        System.err.println("Vendor:    " + b.getErrorCode());
        System.err.print("Update counts:    ");
        int[] updateCounts = b.getUpdateCounts();
        for (int i = 0; i < updateCounts.length; i++) {
            System.err.print(updateCounts[i] + "     ");
        }
    }

    public static void printSQLException(SQLException ex) {
        for (Throwable e : ex) {
            if (e instanceof SQLException) {
                if (ignoreSQLException(((SQLException) e).getSQLState()) == false) {
                    e.printStackTrace(System.err);
                    System.err.println("SQLState: " + ((SQLException) e).getSQLState());
                    System.err.println("Error Code: " + ((SQLException) e).getErrorCode());
                    System.err.println("Message: " + e.getMessage());
                    Throwable t = ex.getCause();
                    while (t != null) {
                        System.out.println("Cause: " + t);
                        t = t.getCause();
                    }
                }
            }
        }
    }

    public static void alternatePrintSQLException(SQLException ex) {
        while (ex != null) {
            System.err.println("SQLState: " + ex.getSQLState());
            System.err.println("Error Code: " + ex.getErrorCode());
            System.err.println("Message: " + ex.getMessage());
            Throwable t = ex.getCause();
            while (t != null) {
                System.out.println("Cause: " + t);
                t = t.getCause();
            }
            ex = ex.getNextException();
        }
    }

    private void setProperties(String fileName)
            throws FileNotFoundException, IOException, InvalidPropertiesFormatException {
        this.prop = new Properties();
        FileInputStream fis = new FileInputStream(fileName);
        prop.loadFromXML(fis);

        this.dbms = this.prop.getProperty("dbms");
        this.jarFile = this.prop.getProperty("jar_file");
        this.driver = this.prop.getProperty("driver");
        this.dbName = this.prop.getProperty("database_name");
        this.userName = this.prop.getProperty("user_name");
        this.password = this.prop.getProperty("password");
        this.serverName = this.prop.getProperty("server_name");
        this.portNumber = Integer.parseInt(this.prop.getProperty("port_number"));

        System.out.println("Set the following properties:");
        System.out.println("dbms: " + dbms);
        System.out.println("driver: " + driver);
        System.out.println("dbName: " + dbName);
        System.out.println("userName: " + userName);
        System.out.println("serverName: " + serverName);
        System.out.println("portNumber: " + portNumber);
    }

    public Connection getConnectionToDatabase() throws SQLException {
        Connection conn = null;
        Properties connectionProps = new Properties();
        connectionProps.put("user", this.userName);
        connectionProps.put("password", this.password);

        // Using a driver manager:
        if (this.dbms.equals("mysql")) {
//            DriverManager.registerDriver(new com.mysql.jdbc.Driver());
            conn = DriverManager.getConnection("jdbc:" + dbms + "://"
                    + serverName + ":" + portNumber + "/" + dbName, connectionProps);
            conn.setCatalog(this.dbName);
        } else if (this.dbms.equals("derby")) {
//            DriverManager.registerDriver(new org.apache.derby.jdbc.EmbeddedDriver());
            conn = DriverManager.getConnection(
                    "jdbc:" + dbms + ":" + dbName, connectionProps);
        }
        System.out.println("Connected to database");
        return conn;
    }

    public Connection getConnection() throws SQLException {
        Connection conn = null;
        Properties connectionProps = new Properties();
        connectionProps.put("user", this.userName);
        connectionProps.put("password", this.password);

        String currentUrlString;

        switch (this.dbms) {
            case "mysql":
                currentUrlString = "jdbc:mysql://" + this.serverName
                        + ":" + this.portNumber + "/";
                conn = DriverManager.getConnection(currentUrlString, connectionProps);
                this.urlString = currentUrlString + this.dbName;
                break;
            case "mysql.pooled":
                currentUrlString = "jdbc:mysql://" + this.serverName
                        + ":" + this.portNumber + "/";
                conn = getMySQLPooledConnection();
                this.urlString = currentUrlString + this.dbName;
                System.out.println("MySQL pooled connection obtained");
                break;
            case "mariadb":
                currentUrlString = "jdbc:mariadb://" + this.serverName
                        + ":" + this.portNumber + "/";
                conn = DriverManager.getConnection(currentUrlString, connectionProps);
                this.urlString = currentUrlString + this.dbName;
                break;
            case "mariadb.pooled":
                currentUrlString = "jdbc:mariadb://" + this.serverName
                        + ":" + this.portNumber + "/";
                conn = getMariaDBPooledConnection();
                this.urlString = currentUrlString + this.dbName;
                System.out.println("MariaDB pooled connection obtained");
                break;
            case "derby":
                this.urlString = "jdbc:" + this.dbms + ":" + this.dbName;
                conn = DriverManager.getConnection(this.urlString + ";create=true", connectionProps);
                break;
            default:
                break;
        }
        
        if (conn != null) {
            System.out.println("Connected to database");
        
            DatabaseMetaData dbMetaData = conn.getMetaData();
            System.out.println("\nDBMS supports TYPE_FORWARD_ONLY ResulSet: " 
                    + dbMetaData.supportsResultSetType(ResultSet.TYPE_FORWARD_ONLY));
            System.out.println("DBMS supports TYPE_SCROLL_INSENSITIVE ResulSet: " 
                    + dbMetaData.supportsResultSetType(ResultSet.TYPE_SCROLL_INSENSITIVE));
            System.out.println("DBMS supports TYPE_SCROLL_SENSITIVE ResulSet: " 
                    + dbMetaData.supportsResultSetType(ResultSet.TYPE_SCROLL_SENSITIVE));
            System.out.println("DBMS supports CONCUR_READ_ONLY in TYPE_SCROLL_INSENSITIVE ResulSet: " 
                    + dbMetaData.supportsResultSetConcurrency(
                            ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY));
            System.out.println("DBMS supports CONCUR_UPDATABLE in TYPE_SCROLL_INSENSITIVE ResulSet: " 
                    + dbMetaData.supportsResultSetConcurrency(
                            ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE));
            JDBCTutorialUtilities.cursorHoldabilitySupport(conn);
            JDBCTutorialUtilities.rowIdLifetime(conn);
        }
        
        return conn;
    }

    public Connection getConnection(String userName, String password) throws SQLException {
        Connection conn = null;
        Properties connectionProps = new Properties();
        connectionProps.put("user", userName);
        connectionProps.put("password", password);
        
        String currentUrlString;
        
        switch (this.dbms) {
            case "mysql":
                currentUrlString = "jdbc:mysql://" + this.serverName
                        + ":" + this.portNumber + "/";
                conn = DriverManager.getConnection(currentUrlString, connectionProps);
                conn.setCatalog(this.dbName);
                break;
            case "mariadb":
                currentUrlString = "jdbc:mariadb://" + this.serverName
                        + ":" + this.portNumber + "/";
                conn = DriverManager.getConnection(currentUrlString, connectionProps);
                conn.setCatalog(this.dbName);
                break;
            case "derby":
                conn = DriverManager.getConnection("jdbc:" + this.dbms + ":" + this.dbName
                        + ";create=true", connectionProps);
                break;
            default:
                break;
        }
        return conn;
    }
    
    public Connection getMariaDBPooledConnection() throws SQLException {
        if (mariaDbPoolDataSource == null) {
            String url = "jdbc:mariadb://" + serverName + ":" + portNumber + "/"
                    + "?user=" + userName + "&password=" + password;
            mariaDbPoolDataSource = new MariaDbPoolDataSource(url);
        }
        PooledConnection pooledConnection = mariaDbPoolDataSource.getPooledConnection();
        
        return pooledConnection.getConnection();
    }
    
    public Connection getMySQLPooledConnection() throws SQLException {
        if (mysqlConnectionPoolDataSource == null) {
            String url = "jdbc:mysql://" + serverName + ":" + portNumber + "/";
            mysqlConnectionPoolDataSource = new MysqlConnectionPoolDataSource();
            mysqlConnectionPoolDataSource.setURL(url);
            mysqlConnectionPoolDataSource.setUser(userName);
            mysqlConnectionPoolDataSource.setPassword(password);
        }
        PooledConnection pooledConnection = mysqlConnectionPoolDataSource.getPooledConnection();
        
        return pooledConnection.getConnection();
    }

    public static void createDatabase(Connection conn, String dbName, String dbms) {

        if (dbms.startsWith("mysql") || dbms.startsWith("mariadb")) {
            try {
                Statement s = conn.createStatement();
                String newDatabaseString
                        = "CREATE DATABASE IF NOT EXISTS " + dbName;
                // String newDatabaseString = "CREATE DATABASE " + dbName;
                s.executeUpdate(newDatabaseString);
                JDBCTutorialUtilities.getWarningsFromStatement(s);
                
                System.out.println("Created database " + dbName);
            } catch (SQLException e) {
                printSQLException(e);
            }
        }
    }

    public static void closeConnection(Connection conn) {
        System.out.println("Releasing all open resources ...");
        try {
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException sqle) {
            printSQLException(sqle);
        }
    }

    public static String convertDocumentToString(Document doc)
            throws TransformerConfigurationException, TransformerException {
        Transformer t = TransformerFactory.newInstance().newTransformer();
//        t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        StringWriter sw = new StringWriter();
        t.transform(new DOMSource(doc), new StreamResult(sw));
        return sw.toString();
    }

    public static void main(String[] args) {
        JDBCTutorialUtilities myJDBCTutorialUtilities;
        Connection myConnection = null;
        
        if (args.length == 0) {
            System.err.println("Properties file not specified at command line");
            return;
        } else {
            try {
                System.out.println("Reading properties file " + args[0]);
                myJDBCTutorialUtilities = new JDBCTutorialUtilities(args[0]);
            } catch (IOException e) {
                System.err.println("Problem reading properties file " + args[0]);
                e.printStackTrace(System.err);
                return;
            }
        }

        try {
            myConnection = myJDBCTutorialUtilities.getConnection();
            // JDBCTutorialUtilities.outputClientInfoProperties(myConnection);
            // myConnection = myJDBCTutorialUtilities.getConnection("root", "root", "jdbc:mysql://localhost:3306/");
            // myConnection = myJDBCTutorialUtilities.
            // getConnectionWithDataSource(myJDBCTutorialUtilities.dbName,"derby","", "", "localhost", 3306);

            // Java DB does not have an SQL create database command; it does require createDatabase
            JDBCTutorialUtilities.createDatabase(myConnection,
                    myJDBCTutorialUtilities.dbName, myJDBCTutorialUtilities.dbms);
            
            myConnection.setCatalog(myJDBCTutorialUtilities.dbName);            
            
            JDBCTutorialUtilities.initializeTables(myConnection, 
                    myJDBCTutorialUtilities.dbName, myJDBCTutorialUtilities.dbms);
            
            System.out.println();
            CoffeesTable.viewTable(myConnection);
            System.out.println();


        } catch (SQLException e) {
            JDBCTutorialUtilities.printSQLException(e);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        } finally {
            JDBCTutorialUtilities.closeConnection(myConnection);
        }
    }
}
