package simple_seata_demo_01.service;

import org.apache.seata.spring.annotation.GlobalTransactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

@Service
public class AccountService {
    @Autowired
    public DataSource dataSource;

    private final JdbcTemplate jdbcTemplate;

    public AccountService(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @GlobalTransactional
    public void transferCommit(int fromId, int toId, int amount) {
        jdbcTemplate.update("UPDATE account SET balance = balance - ? WHERE id = ?", amount, fromId);
        jdbcTemplate.update("UPDATE account SET balance = balance + ? WHERE id = ?", amount, toId);
    }

    @GlobalTransactional
    public void transferRollback(int fromId, int toId, int amount) {
        jdbcTemplate.update("UPDATE account SET balance = balance - ? WHERE id = ?", amount, fromId);
        // 模拟业务异常触发回滚
        throw new RuntimeException("Artificial exception for rollback");
    }
}