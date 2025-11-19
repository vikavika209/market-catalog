package market.repo.jdbc;

import market.domain.Role;
import market.domain.User;
import market.repo.UserRepository;
import org.junit.jupiter.api.*;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Интеграционные тесты для UserRepositoryJdbc с использованием Testcontainers.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserRepositoryJdbcTest extends BaseJdbcTest {

    private static UserRepository userRepo;

    @BeforeAll
    static void start() {
        userRepo = new UserRepositoryJdbc(ds);
    }

    @Test
    @Order(1)
    void saveUser_shouldInsertNewUser_andAssignId() {
        User u = new User();
        u.setUsername("new_user");
        u.setPassword("secret");
        u.setRole(Role.USER);

        assertNull(u.getId(), "До сохранения ID должен быть null");

        userRepo.saveUser(u);

        assertNotNull(u.getId(), "После saveUser() ID должен быть установлен");

        Optional<User> loaded = userRepo.findByUsername("new_user");
        assertTrue(loaded.isPresent(), "Пользователь должен находиться в БД");
        assertEquals("new_user", loaded.get().getUsername());
        assertEquals("secret", loaded.get().getPassword());
        assertEquals(Role.USER, loaded.get().getRole());
    }

    @Test
    @Order(2)
    void exists_shouldReturnTrueForExistingUser_andFalseForMissing() {
        assertTrue(userRepo.exists("new_user"), "Пользователь new_user должен существовать");
        assertFalse(userRepo.exists("missing_user_xyz"), "Несуществующий пользователь не должен находиться");
    }

    @Test
    @Order(3)
    void saveUser_shouldUpdateExistingUser_whenIdIsSet() {
        Optional<User> opt = userRepo.findByUsername("new_user");
        assertTrue(opt.isPresent(), "Пользователь new_user должен существовать перед обновлением");

        User u = opt.get();
        Long oldId = u.getId();

        u.setPassword("new_password");
        u.setRole(Role.ADMIN);

        userRepo.saveUser(u);

        Optional<User> reloaded = userRepo.findByUsername("new_user");
        assertTrue(reloaded.isPresent(), "Пользователь должен существовать после обновления");

        User updated = reloaded.get();
        assertEquals(oldId, updated.getId(), "ID пользователя не должен меняться при обновлении");
        assertEquals("new_password", updated.getPassword(), "Пароль должен быть обновлён");
        assertEquals(Role.ADMIN, updated.getRole(), "Роль должна быть обновлена");
    }

    @Test
    @Order(4)
    void findByUsername_shouldReturnEmptyForUnknownUser() {
        Optional<User> opt = userRepo.findByUsername("definitely_unknown_user_12345");
        assertTrue(opt.isEmpty(), "Для неизвестного username должен возвращаться Optional.empty()");
    }

}