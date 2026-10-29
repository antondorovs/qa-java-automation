package io.github.antondorovs.qa.db;

import io.github.antondorovs.qa.testdata.OrderTestData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;

import static org.junit.jupiter.api.Assertions.*;

@Tag("db")
@Testcontainers
class OrderDatabaseTest {
    @Container
    private static final PostgreSQLContainer DATABASE = new PostgreSQLContainer("postgres:17.6-alpine")
            .withDatabaseName("qa_orders")
            .withInitScript("db/schema.sql");

    private Connection connection;
    private DatabaseFixture fixture;
    private OrderTestData data;
    private long customerId;

    @BeforeEach
    void createTestData() throws SQLException {
        connection = DriverManager.getConnection(DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword());
        connection.setAutoCommit(false);
        try (PreparedStatement query = connection.prepareStatement("SELECT COUNT(*) FROM customers");
             ResultSet result = query.executeQuery()) {
            result.next();
            assertEquals(0, result.getInt(1), "Previous test must leave no customer data");
        }
        fixture = new DatabaseFixture(connection);
        data = OrderTestData.newOrder();
        customerId = fixture.createCustomer(data.customerName(), data.customerEmail());
    }

    @AfterEach
    void cleanUp() throws SQLException {
        if (connection != null) {
            try {
                connection.rollback();
            } finally {
                connection.close();
            }
        }
    }

    @Test
    void storesOrderWithCustomerDetails() throws SQLException {
        long orderId = fixture.createOrder(customerId, data.total(), "NEW");
        String sql = """
                SELECT o.id, o.total, o.status, o.created_at, c.name, c.email
                FROM orders o
                JOIN customers c ON c.id = o.customer_id
                WHERE o.id = ?
                """;
        try (PreparedStatement query = connection.prepareStatement(sql)) {
            query.setLong(1, orderId);
            try (ResultSet result = query.executeQuery()) {
                assertTrue(result.next());
                assertEquals(orderId, result.getLong("id"));
                assertEquals(data.total(), result.getBigDecimal("total"));
                assertEquals("NEW", result.getString("status"));
                assertEquals(data.customerName(), result.getString("name"));
                assertEquals(data.customerEmail(), result.getString("email"));
                assertNotNull(result.getTimestamp("created_at"));
                assertFalse(result.next());
            }
        }
    }

    @Test
    void updatesOrderStatusWithoutChangingTotal() throws SQLException {
        long orderId = fixture.createOrder(customerId, data.total(), "NEW");
        try (PreparedStatement update = connection.prepareStatement("UPDATE orders SET status = ? WHERE id = ?")) {
            update.setString(1, "PAID");
            update.setLong(2, orderId);
            assertEquals(1, update.executeUpdate());
        }
        try (PreparedStatement query = connection.prepareStatement("SELECT status, total FROM orders WHERE id = ?")) {
            query.setLong(1, orderId);
            try (ResultSet result = query.executeQuery()) {
                assertTrue(result.next());
                assertEquals("PAID", result.getString("status"));
                assertEquals(data.total(), result.getBigDecimal("total"));
            }
        }
    }

    @Test
    void deletesOrder() throws SQLException {
        long orderId = fixture.createOrder(customerId, data.total(), "NEW");
        try (PreparedStatement delete = connection.prepareStatement("DELETE FROM orders WHERE id = ?")) {
            delete.setLong(1, orderId);
            assertEquals(1, delete.executeUpdate());
        }
        try (PreparedStatement query = connection.prepareStatement("SELECT id FROM orders WHERE id = ?")) {
            query.setLong(1, orderId);
            try (ResultSet result = query.executeQuery()) {
                assertFalse(result.next());
            }
        }
    }

    @Test
    void rejectsDuplicateCustomerEmail() {
        SQLException error = assertThrows(SQLException.class,
                () -> fixture.createCustomer("Another Customer", data.customerEmail()));
        assertEquals("23505", error.getSQLState());
    }

    @Test
    void rejectsOrderWithoutExistingCustomer() {
        SQLException error = assertThrows(SQLException.class,
                () -> fixture.createOrder(-1, data.total(), "NEW"));
        assertEquals("23503", error.getSQLState());
    }

    @Test
    void preservesOrderWhenCustomerDeletionIsRejected() throws SQLException {
        long orderId = fixture.createOrder(customerId, data.total(), "NEW");
        Savepoint beforeDelete = connection.setSavepoint();

        SQLException error = assertThrows(SQLException.class, () -> {
            try (PreparedStatement delete = connection.prepareStatement("DELETE FROM customers WHERE id = ?")) {
                delete.setLong(1, customerId);
                delete.executeUpdate();
            }
        });
        assertEquals("23503", error.getSQLState());
        connection.rollback(beforeDelete);

        try (PreparedStatement query = connection.prepareStatement(
                "SELECT o.id FROM orders o JOIN customers c ON c.id = o.customer_id WHERE o.id = ? AND c.id = ?")) {
            query.setLong(1, orderId);
            query.setLong(2, customerId);
            try (ResultSet result = query.executeQuery()) {
                assertTrue(result.next(), "Rejected customer deletion must preserve the order and its customer");
                assertEquals(orderId, result.getLong("id"));
                assertFalse(result.next());
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.00", "-1.00"})
    void rejectsNonPositiveOrderTotal(String amount) {
        SQLException error = assertThrows(SQLException.class,
                () -> fixture.createOrder(customerId, new BigDecimal(amount), "NEW"));
        assertEquals("23514", error.getSQLState());
    }

    @Test
    void rejectsUnknownOrderStatus() {
        SQLException error = assertThrows(SQLException.class,
                () -> fixture.createOrder(customerId, data.total(), "UNKNOWN"));
        assertEquals("23514", error.getSQLState());
    }
}
