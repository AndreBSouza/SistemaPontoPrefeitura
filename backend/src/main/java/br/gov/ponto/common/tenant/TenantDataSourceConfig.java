package br.gov.ponto.common.tenant;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Envolve o DataSource autoconfigurado com {@link TenantAwareDataSource} para que
 * toda conexao carregue a GUC de tenant usada pelas policies de RLS.
 */
@Configuration
public class TenantDataSourceConfig {

    @Bean
    static BeanPostProcessor tenantDataSourceBeanPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof DataSource dataSource && !(bean instanceof TenantAwareDataSource)) {
                    return new TenantAwareDataSource(dataSource);
                }
                return bean;
            }
        };
    }
}
