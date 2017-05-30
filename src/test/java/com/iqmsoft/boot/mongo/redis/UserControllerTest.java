package com.iqmsoft.boot.mongo.redis;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.http.MockHttpOutputMessage;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import com.iqmsoft.boot.mongo.redis.User;
import com.iqmsoft.boot.mongo.redis.UserRepository;
import com.iqmsoft.boot.mongo.redis.UserServiceApplication;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;

import de.flapdoodle.embed.mongo.Command;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.DownloadConfigBuilder;
import de.flapdoodle.embed.mongo.config.ExtractedArtifactStoreBuilder;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.config.RuntimeConfigBuilder;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.config.IRuntimeConfig;
import de.flapdoodle.embed.process.extract.UserTempNaming;
import de.flapdoodle.embed.process.runtime.Network;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;

import java.util.Arrays;
import java.util.Date;

import static org.hamcrest.Matchers.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@ActiveProfiles(value = "test")
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@WebAppConfiguration

public class UserControllerTest {

	private MediaType contentType = new MediaType(MediaType.APPLICATION_JSON.getType(),
			MediaType.APPLICATION_JSON.getSubtype(), Charset.forName("utf8"));

	private MockMvc mockMvc;

	private String uri = "/api/user";

	@SuppressWarnings("rawtypes")
	private HttpMessageConverter mappingJackson2HttpMessageConverter;

	@Autowired
	private UserRepository repository;

	@Autowired
	private WebApplicationContext webApplicationContext;

	// Mongo DB embedded
	private static MongodExecutable _mongodExe;
	private static MongodProcess _mongod;
	private static MongoClient _mongo;

	private static User user = null;
	private static User yetAnotherUser = null;
	private static User changedUser = null;
	
	private static String bindIp = "localhost";

	@Autowired
	void setConverters(HttpMessageConverter<?>[] converters) {

		this.mappingJackson2HttpMessageConverter = Arrays.asList(converters).stream()
				.filter(hmc -> hmc instanceof MappingJackson2HttpMessageConverter).
				findAny().get();

		Assert.assertNotNull("the JSON message converter must not be null", 
				this.mappingJackson2HttpMessageConverter);
	}

	@BeforeClass
	public static void beforeAll() throws Exception {
		// setup test data
		// setup test data
		user = User.builder().username("user000001").password("password").
				firstname("First Name User 01")
				.lastname("Last Name User 01").dob(new Date()).build();

		yetAnotherUser = User.builder().username("user000002").
				password("password02")
				.firstname("First Name User 02")
				.lastname("Last Name User 02")
				.dob(new Date())
				.build();

		changedUser = User.builder().username("user000001").
				password("password").firstname("Changed First Name User 01")
				.lastname("Changed Last Name User 01")
				.dob(new Date())
				.build();

		// set up embedded mongo db
		Command command = Command.MongoD;

		IRuntimeConfig runtimeConfig = new RuntimeConfigBuilder().defaults(command)
				.artifactStore(new ExtractedArtifactStoreBuilder().defaults(command)
						.download(new DownloadConfigBuilder().
								defaultsForCommand(command).build())
						.executableNaming(new UserTempNaming()))
				.build();

		IMongodConfig mongodConfig = new MongodConfigBuilder().
				version(Version.Main.PRODUCTION)
				.net(new Net(bindIp, 28000, Network.localhostIsIPv6())).build();

		MongodStarter starter = MongodStarter.getInstance(runtimeConfig);

		_mongodExe = starter.prepare(mongodConfig);
		_mongod = _mongodExe.start();
		_mongo = new MongoClient("localhost", 28000);

	}

	@Before
	public void setup() throws Exception {

		this.repository.deleteAll();

		this.mockMvc = webAppContextSetup(webApplicationContext).build();

	}

	@AfterClass
	public static void tearDown() throws Exception {

		_mongod.stop();
		_mongodExe.stop();
	}

	@Test
	public void testForRetrievingAllUser() throws Exception {
		this.repository.insert(user);
		this.repository.insert(yetAnotherUser);
		mockMvc.perform(get(new URI(uri)).contentType(contentType)).
		andExpect(jsonPath("$", hasSize(2)))
				.andExpect(status().isOk());
	}

	@Test
	public void testForCreateUser() throws Exception {

		String userJson = this.json(user);
		mockMvc.perform(post(new URI(uri)).content(userJson).
				contentType(contentType)).andExpect(status().isCreated());
	}

	@Test
	public void testForDuplicateUser() throws Exception {
		this.repository.insert(user);
		String userJson = this.json(user);
		mockMvc.perform(post(new URI(uri)).content(userJson).
				contentType(contentType)).andExpect(status().isLocked());
	}

	@Test
	public void testForChangeUser() throws Exception {
		this.repository.insert(user);
		String userJson = this.json(changedUser);
		mockMvc.perform(
				put(new URI(uri + "/username/" + user.getUsername())).
				content(userJson).contentType(contentType))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.firstname", is("Changed First Name User 01")))
				.andExpect(jsonPath("$.lastname", is("Changed Last Name User 01")));
	}

	@Test
	public void testForUserNameFoundByUsername() throws Exception {
		this.repository.insert(user);
		mockMvc.perform(get(new URI(uri + "/username/" + user.getUsername()))
				.contentType(contentType))
				.andExpect(status().isOk());
	}

	@Test
	public void testForUserNameNotFoundByUsername() throws Exception {
		mockMvc.perform(get(new URI(uri + "/username/" + "fakename"))
				.contentType(contentType))
				.andExpect(status().isOk());
	}

	@Test
	public void testForDeleteByUsername() throws Exception {
		this.repository.insert(user);
		mockMvc.perform(delete(new URI(uri + "/username/" + user.getUsername()))
				.contentType(contentType))
				.andExpect(status().isOk());
	}

	@SuppressWarnings("unchecked")
	protected String json(Object o) throws IOException {
		MockHttpOutputMessage mockHttpOutputMessage = new MockHttpOutputMessage();
		this.mappingJackson2HttpMessageConverter
		.write(o, MediaType.APPLICATION_JSON, mockHttpOutputMessage);
		return mockHttpOutputMessage.getBodyAsString();
	}

	public Mongo getMongo() {
		return _mongo;
	}

}
