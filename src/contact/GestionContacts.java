package contact;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class GestionContacts {
    private final Connection connection;

    public GestionContacts() throws SQLException {
        DatabaseConnection dbConnection = DatabaseConnection.getInstance(
                "jdbc:mysql://sql.freedb.tech:3306/freedb_atelier", // URL de la base de données MySQL
                "freedb_tatiana", // Nom d'utilisateur MySQL
                "zSuadU&38E!tAE6" // Mot de passe MySQL
        );
        this.connection = dbConnection.getConnection();
        createTablesIfNotExist();
    }

    private void createTablesIfNotExist() throws SQLException {
        String createTableSQL = """
            CREATE TABLE IF NOT EXISTS contacts (
                id  INT AUTO_INCREMENT PRIMARY KEY,
                nom TEXT NOT NULL,
                numero_telephone TEXT NOT NULL,
                email TEXT NOT NULL,
                date_anniversaire TEXT,
                type TEXT NOT NULL,
                adresse TEXT,
                nom_entreprise TEXT
            );
        """;
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTableSQL);
        }
    }

    public void ajouterContact(Contact contact) throws SQLException {
        String insertSQL = "INSERT INTO contacts (id, nom, numero_telephone, email, date_anniversaire, type, adresse, nom_entreprise) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(insertSQL)) {
            pstmt.setString(1, contact.getId());
            pstmt.setString(2, contact.getNom());
            pstmt.setString(3, contact.getNumeroTelephone());
            pstmt.setString(4, contact.getEmail());
            pstmt.setString(5, contact.getDateAnniversaire() != null ? contact.getDateAnniversaire().toString() : null);
            pstmt.setString(6, contact instanceof ContactPersonnel ? "personnel" : "professionnel");
            if (contact instanceof ContactPersonnel) {
                pstmt.setString(7, ((ContactPersonnel) contact).getAdresse());
                pstmt.setString(8, null);
            } else {
                pstmt.setString(7, null);
                pstmt.setString(8, ((ContactProfessionnel) contact).getNomEntreprise());
            }
            pstmt.executeUpdate();
        }
    }

    public void supprimerContact(String id) throws SQLException {
        String deleteSQL = "DELETE FROM contacts WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(deleteSQL)) {
            pstmt.setString(1, id);
            pstmt.executeUpdate();
        }
    }

    public void modifierContact(String id, String nom, String numeroTelephone, String email, LocalDate dateAnniversaire) throws SQLException {
        String updateSQL = "UPDATE contacts SET nom = ?, numero_telephone = ?, email = ?, date_anniversaire = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(updateSQL)) {
            pstmt.setString(1, nom);
            pstmt.setString(2, numeroTelephone);
            pstmt.setString(3, email);
            pstmt.setString(4, dateAnniversaire != null ? dateAnniversaire.toString() : null);
            pstmt.setString(5, id);
            pstmt.executeUpdate();
        }
    }

    public Contact rechercherContactParNom(String nom) throws SQLException {
        String selectSQL = "SELECT * FROM contacts WHERE nom = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(selectSQL)) {
            pstmt.setString(1, nom);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return createContactFromResultSet(rs);
                }
            }
        }
        return null;
    }

    public Contact rechercherContactParNumero(String numero) throws SQLException {
        String selectSQL = "SELECT * FROM contacts WHERE numero_telephone = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(selectSQL)) {
            pstmt.setString(1, numero);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return createContactFromResultSet(rs);
                }
            }
        }
        return null;
    }

    public List<Contact> listerContactsParInitiale(char initiale) throws SQLException {
        String selectSQL = "SELECT * FROM contacts WHERE nom LIKE ?";
        List<Contact> contacts = new ArrayList<>();
        try (PreparedStatement pstmt = connection.prepareStatement(selectSQL)) {
            pstmt.setString(1, initiale + "%");
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    contacts.add(createContactFromResultSet(rs));
                }
            }
        }
        return contacts;
    }

    public int getNombreTotalContacts() throws SQLException {
        String countSQL = "SELECT COUNT(*) FROM contacts";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(countSQL)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    public List<Contact> listerContactsParType(Class<?> type) throws SQLException {
        String selectSQL = "SELECT * FROM contacts WHERE type = ?";
        List<Contact> contacts = new ArrayList<>();
        try (PreparedStatement pstmt = connection.prepareStatement(selectSQL)) {
            pstmt.setString(1, type == ContactPersonnel.class ? "personnel" : "professionnel");
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    contacts.add(createContactFromResultSet(rs));
                }
            }
        }
        return contacts;
    }

    public Contact afficherDetailsContactParId(String id) throws SQLException {
        String selectSQL = "SELECT * FROM contacts WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(selectSQL)) {
            pstmt.setString(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return createContactFromResultSet(rs);
                }
            }
        }
        return null;
    }

    public List<Contact> listerContactsAvecAnniversaireDansPeriode(LocalDate debut, LocalDate fin) throws SQLException {
        String selectSQL = "SELECT * FROM contacts WHERE date_anniversaire BETWEEN ? AND ?";
        List<Contact> contacts = new ArrayList<>();
        try (PreparedStatement pstmt = connection.prepareStatement(selectSQL)) {
            pstmt.setString(1, debut.toString());
            pstmt.setString(2, fin.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    contacts.add(createContactFromResultSet(rs));
                }
            }
        }
        return contacts;
    }

    private Contact createContactFromResultSet(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String nom = rs.getString("nom");
        String numeroTelephone = rs.getString("numero_telephone");
        String email = rs.getString("email");
        LocalDate dateAnniversaire = rs.getString("date_anniversaire") != null ? LocalDate.parse(rs.getString("date_anniversaire")) : null;
        String type = rs.getString("type");
        if ("personnel".equals(type)) {
            String adresse = rs.getString("adresse");
            return new ContactPersonnel(id, nom, numeroTelephone, email, dateAnniversaire, adresse);
        } else {
            String nomEntreprise = rs.getString("nom_entreprise");
            return new ContactProfessionnel(id, nom, numeroTelephone, email, dateAnniversaire, nomEntreprise);
        }
    }

    // Pas besoin de méthode close ici, la gestion de la connexion est dans DatabaseConnection
}