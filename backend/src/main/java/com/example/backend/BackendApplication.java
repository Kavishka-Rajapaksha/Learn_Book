
package com.example.backend;

import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.MongoDatabaseFactory;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

	@Bean
	public GridFSBucket gridFSBucket(MongoDatabaseFactory mongoDatabaseFactory) {
		return GridFSBuckets.create(mongoDatabaseFactory.getMongoDatabase(), "media");
	}
}