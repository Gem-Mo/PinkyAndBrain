import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * JDBC Aufgabe 3d
 *
 * Aktualisieren des Datenbankschemas.
 *
 * Ziele
 * - Arbeiten mit DDL-Befehlen
 * - Erstellen von überwachten Fremdschlüsselbeziehungen
 * - Erste Heranführung an das Thema Normalisierung, hier besonders deren technische Umsetzung
 *
 * In dieser Datei sollen Sie:
 * Redundanzen in der Tabelle Teilestamm, die in der Spalte 'farbe' zu finden sind,
 * vermeiden. Dazu soll eine neue Tabelle 'farbe' angelegt werden.
 * Die Tabelle Teilestamm soll dann für die neue Tabelle 'farbe' einen
 * Fremdschlüssel verwenden.
 * Bei dieser Gelegenheit werden die Farben in der Tabelle 'farbe' um
 * zusätzliche Informationen ergänzt.
 *
 * Die Tabelle 'farbe' bekommt das folgende physikalische Datenmodell:
 *
 * 	Spalte	Beschreibung
 * 	nr		Der Type ist INTEGER, automatisches Hochzählen ist erlaubt, aber
 *  		nicht notwendig (es gibt leider keine einheitliche Syntax, die zwischen Oracle, MySQL
 *  		und PostgreSQL kompatibel ist). Diese Spalte bildet den
 *  		Primärschlüssel.
 *	name	Hat die gleichen Eigenschaften wie 'teilestamm.farbe', aber die Spalte
 *			darf keine Duplikate enthalten.
 *	rot, gruen, blau
 *			Diese Spalten sind vom Typ REAL in einem Wertebereich von
 *			[0.0; 1.0], der sichergestellt werden muss.
 *			Der Standardwert ist 0.
 *
 * Test Ausgabe
 *
 * Die folgende Ausgabe sollte auf System.out erscheinen, wenn die main()
 * Methode zum ersten mal aufgerufen wird:
 * Updating database layout ...
 * Table 'farbe' created.
 * Added 3 rows to 'farbe'
 * Column 'farbnr' added to table 'teilestamm'
 * Set 'teilestamm.farbnr' in 34 rows
 * Column 'farbe' removed from 'teilestamm'
 *
 * Hinweis:
 * Setzen Sie die Methode LabUtilities.reInitializeDB() ein,
 * um die Datenbank immer wieder neu aufzusetzen beim Testen.
 */
public class SQLUpdateManager  {

    /**
     * Die verwendete SQL Verbindung.
     */
    private Connection connection;
    private Statement statement;

    /**
     * Der Konstruktor, löst den Update-Vorgang aus.
     * <p>
     * Stellt die Verbindung zur Datenbank her und schließt diese auch wieder.
     *
     * @throws SQLException Wird geworfen, wenn die Datenbankverbindung oder ein
     *                      Statement scheitert
     */
    public SQLUpdateManager() throws SQLException {
        // TODO begin
        SQLConnector connector = new SQLConnector();
        this.connection = connector.getConnection(this);
        // TODO end

        if (!hasTable("farbe")) {
            update();
        } else {
            String err = "Table 'farbe' already created!";
            System.err.println(err);
            throw new SQLException(err);
        }
        // TODO begin
        this.connection.close();
        // TODO end
    }

    /**
     * Prüft, ob eine Tabelle existiert.
     *
     * @param table Die zu prüfende Tabelle
     * @return True, falls die Tabelle existiert, sonst False
     * @throws SQLException Im Fall von Verbindungsproblemen
     */
    private boolean hasTable(String table) throws SQLException {
        // TODO begin
        statement = connection.createStatement();
        try{
            ResultSet supp = statement.executeQuery("SELECT * FROM " + table + " ;");
            statement.close();

        } catch (SQLSyntaxErrorException e){
            System.out.println("Tabelle existiert nicht");
            return false;
        }
        return true;
        // TODO end
    }

    /**
     * Aktualisiere das Datenbanklayout.<p>
     *
     * Führt die folgenden Aktionen aus:
     * - Geeignete Transaktions-Isolationsebene setzen ...
     * - Tabelle farbe anlegen
     * - Vorhandene Farben von teilestamm.farbe in farbe.name kopieren
     * - RGB Werte zu farbe Einträgen setzen
     * - In teilestamm die Spalte farbnr (als Foreign Key) anlegen
     * - Die Spalte teilestamm.farbnr mit Werten befüllen
     * - Die Spalte teilestamm.farbe entfernen
     * - Im Erfolgsfall Änderungen committen, sonst zurückrollen
     *
     * @throws SQLException Im Fall von Verbindungsproblemen
     */
    private void update() throws SQLException {
        System.out.println("Updating database layout ...");

        // TODO begin
        //Geeignete Transaktions-Isolationsebene setzen
        connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
        connection.setAutoCommit(false);
        
        //Tabelle farbe anlegen
        statement = connection.createStatement();
        connection.setSavepoint();

        try {
            statement.executeUpdate("DROP TABLE farbe");

        statement.executeUpdate("CREATE TABLE farbe(nr INT PRIMARY KEY, name VARCHAR(32) UNIQUE NOT NULL,"
                + "rot FLOAT DEFAULT 0.0 CHECK(rot >= 0.0 AND rot <= 1.0), gruen FLOAT DEFAULT 0.0 CHECK(gruen >= 0.0 AND gruen <= 1.0),"
                + "blau FLOAT DEFAULT 0.0 CHECK(blau >= 0.0 AND blau <= 1.0))");
        System.out.println("Table 'farbe' created");
        
        //Vorhandene Farben von teilestamm.farbe in farbe.name kopieren
        ResultSet result = statement.executeQuery("SELECT farbe FROM teilestamm WHERE farbe IS NOT NULL GROUP BY farbe ORDER BY farbe ASC");
        int i = 1;
        statement.clearBatch();
        while (result.next()) {
            statement.addBatch("INSERT INTO farbe (nr, name) VALUES (" + (i++) + ", '" + result.getString(1).trim() + "')");
        }
        statement.executeBatch();
        result.close();
        System.out.println(i - 1   + " rows added to 'farbe'");
        
        //RGB Werte zu farbe Einträgen setzen
        statement.clearBatch();
        statement.addBatch("UPDATE farbe SET rot = 0.0, gruen = 0.0, blau = 0.0 WHERE name = 'schwarz'");
        statement.addBatch("UPDATE farbe SET rot = 1.0, gruen = 0.0, blau = 0.0 WHERE name = 'rot'");
        statement.addBatch("UPDATE farbe SET rot = 0.0, gruen = 0.0, blau = 1.0 WHERE name = 'blau'"); 
        int[] affectedBatch = statement.executeBatch();
        int sum = 0;
        for (i = 0; i < affectedBatch.length; i++){
            sum += affectedBatch[i];
          }
        System.out.println("Updated " + sum + " rows.");
        
        
        //In teilestamm die Spalte farbnr (als Foreign Key) anlegen
        statement = connection.createStatement();
        statement.executeUpdate("ALTER TABLE teilestamm ADD farbnr INT REFERENCES farbe (nr)");
        System.out.println("Column 'farbnr' added to table 'teilestamm'");
        
        
        //Die Spalte teilestamm.farbnr mit Werten befüllen
        statement.clearBatch();
        sum = statement.executeUpdate("UPDATE teilestamm SET teilestamm.farbnr = (SELECT farbnr FROM farbe WHERE teilestamm.farbe = farbe.name)"
                + "WHERE teilestamm.farbe IS NOT NULL");
        System.out.println("Set 'teilestamm.farbnr' in " + sum + " rows.");
        
        //Die Spalte teilestamm.farbe entfernen
        statement = connection.createStatement();
        statement.executeUpdate("ALTER TABLE teilestamm DROP COLUMN farbe");
        System.out.println("Column 'farbe' removed from 'teilestamm'");
 
        connection.commit();
        // TODO end
        } catch (SQLException e) {
            connection.rollback();
            e.printStackTrace();
        }

        statement.close();
        connection.close();
    }

    /**
     * Diese Methode wird zum Testen der Implementierung verwendet.
     *
     * @param args Kommandozeilenargumente, nicht verwendet
     * @throws SQLException Bei jedem SQL Fehler
     */
    public static void main(String[] args) throws SQLException {
        new SQLUpdateManager();
    }
}
