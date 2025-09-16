package simple_tcc_demo_02.service;

import org.apache.seata.spring.annotation.GlobalTransactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author zero
 * @description todo
 * @date 2025-09-12
 */
@Service
public class EntryService {

    @Autowired
    private AccountTccService accountTccService;

    @GlobalTransactional(rollbackFor = Exception.class) // 声明事务边界。
    public boolean tryDeduct(String accountId, int amount) {
        // 执行 TCC 扣款
        return accountTccService.tryDeduct(accountId, amount);
    }

    public int getFrozenAmount(String accountId) {
        return accountTccService.getFrozenAmount(accountId);
    }

    public void clearFrozenAmounts() {
        accountTccService.clearFrozenAmounts();
    }
}
