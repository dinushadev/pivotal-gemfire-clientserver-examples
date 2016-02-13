package io.pivotal.gemfire.main;

import java.util.Properties;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.gemfire.CacheFactoryBean;
import org.springframework.data.gemfire.PartitionedRegionFactoryBean;
import org.springframework.data.gemfire.RegionAttributesFactoryBean;
import org.springframework.data.gemfire.server.CacheServerFactoryBean;

import com.gemstone.gemfire.cache.Cache;
import com.gemstone.gemfire.cache.CacheLoader;
import com.gemstone.gemfire.cache.CacheLoaderException;
import com.gemstone.gemfire.cache.LoaderHelper;
import com.gemstone.gemfire.cache.RegionAttributes;

/**
 * The SpringGemFireServerApplication class...
 *
 * @author John Blum
 * @since 1.0.0
 */
@ConfigurationProperties
@SpringBootApplication
@SuppressWarnings("unused")
public class SpringGemFireServerApplication {

	static final boolean DEFAULT_AUTO_STARTUP = true;

	public static final int DEFAULT_MAX_CONNECTIONS = 100;

	public static void main(final String[] args) {
		SpringApplication.run(SpringGemFireServerApplication.class, args);
	}

	@Bean
	Properties gemfireProperties(@Value("${spring.gemfire.log-level:config}") String logLevel,
		@Value("${spring.gemfire.locator:localhost[11235]}") String locatorHostPort,
		@Value("${spring.gemfire.manager.port:1199}") String managerPort)
	{
		Properties gemfireProperties = new Properties();

		gemfireProperties.setProperty("name", SpringGemFireServerApplication.class.getSimpleName());
		gemfireProperties.setProperty("mcast-port", "0");
		gemfireProperties.setProperty("log-level", logLevel);
		gemfireProperties.setProperty("locators", locatorHostPort);
		gemfireProperties.setProperty("start-locator", locatorHostPort);
		gemfireProperties.setProperty("jmx-manager", "true");
		gemfireProperties.setProperty("jmx-manager-port", managerPort);
		gemfireProperties.setProperty("jmx-manager-start", "true");

		return gemfireProperties;
	}

	@Bean
	CacheFactoryBean gemfireCache(@Qualifier("gemfireProperties") Properties gemfireProperties) {
		CacheFactoryBean gemfireCache = new CacheFactoryBean();

		gemfireCache.setClose(true);
		gemfireCache.setLazyInitialize(true);
		gemfireCache.setProperties(gemfireProperties);
		gemfireCache.setUseBeanFactoryLocator(false);

		return gemfireCache;
	}

	@Bean
	CacheServerFactoryBean gemfireCacheServer(Cache gemfireCache,
		@Value("${spring.gemfire.cache.server.bind-address:localhost}") String bindAddress,
		@Value("${spring.gemfire.cache.server.hostname-for-clients:localhost}") String hostNameForClients,
		@Value("${spring.gemfire.cache.server.port:12480}") int port)
	{
		CacheServerFactoryBean gemfireCacheServer = new CacheServerFactoryBean();

		gemfireCacheServer.setCache(gemfireCache);
		gemfireCacheServer.setAutoStartup(DEFAULT_AUTO_STARTUP);
		gemfireCacheServer.setBindAddress(bindAddress);
		gemfireCacheServer.setHostNameForClients(hostNameForClients);
		gemfireCacheServer.setPort(port);
		gemfireCacheServer.setMaxConnections(DEFAULT_MAX_CONNECTIONS);

		return gemfireCacheServer;
	}

	@Bean
	PartitionedRegionFactoryBean<Long, Long> factorialsRegion(Cache gemfireCache,
		@Qualifier("factorialsRegionAttributes") RegionAttributes<Long, Long> factorialsRegionAttributes)
	{
		PartitionedRegionFactoryBean<Long, Long> factorialsRegion = new PartitionedRegionFactoryBean<>();

		factorialsRegion.setCache(gemfireCache);
		factorialsRegion.setClose(false);
		factorialsRegion.setAttributes(factorialsRegionAttributes);
		factorialsRegion.setName("Factorials");
		factorialsRegion.setPersistent(false);

		return factorialsRegion;
	}

	@Bean
	@SuppressWarnings("unchecked")
	RegionAttributesFactoryBean factorialsRegionAttributes() {
		RegionAttributesFactoryBean factorialsRegionAttributes = new RegionAttributesFactoryBean();

		factorialsRegionAttributes.setCacheLoader(factorialsCacheLoader());
		factorialsRegionAttributes.setKeyConstraint(Long.class);
		factorialsRegionAttributes.setValueConstraint(Long.class);

		return factorialsRegionAttributes;
	}

	@Bean
	FactorialsCacheLoader factorialsCacheLoader() {
		return new FactorialsCacheLoader();
	}

	class FactorialsCacheLoader implements CacheLoader<Long, Long> {

		// stupid, naive implementation of Factorial!
		public Long load(LoaderHelper<Long, Long> loaderHelper) throws CacheLoaderException {
			long number = loaderHelper.getKey();

			assert number >= 0 : String.format("number [%1$d] must be greater than equal to 0", number);

			if (number < 3) {
				return (number < 2 ? 1l : 2l);
			}

			long result = number;

			while (number-- > 1) {
				result *= number;
			}

			return result;
		}

		public void close() {
		}
	}

}