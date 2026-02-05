package io.braineous.dd.llm.cr.persistence;

import ai.braineous.rag.prompt.observe.Console;
import com.mongodb.client.MongoClient;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
public class CommitSentMongoStoreIT {

    @Inject
    CommitSentMongoStore store;

    @Inject
    MongoClient mongoClient;

    @BeforeEach
    public void setup() {
        Console.log("CommitSentMongoStoreIT.setup", "clearing collection");
        store.clear();
    }

    @Test
    public void append_inserts_document_verbatim() {

        Console.log("TEST", "append_inserts_document_verbatim");

        // given
        String commitId = "c-1";
        String payload = "{\"x\":1,\"y\":\"z\"}";

        long before = store.count();
        Console.log("count.before", before);

        // when
        store.append(commitId, payload);

        // then
        long after = store.count();
        Console.log("count.after", after);

        assertEquals(before + 1, after);
    }

    @Test
    public void append_ignores_null_or_blank_inputs() {

        Console.log("TEST", "append_ignores_null_or_blank_inputs");

        long before = store.count();
        Console.log("count.before", before);

        // nulls
        store.append(null, "{\"x\":1}");
        store.append("c-1", null);

        // blanks
        store.append("   ", "{\"x\":1}");
        store.append("c-1", "   ");

        long after = store.count();
        Console.log("count.after", after);

        assertEquals(before, after);
    }
}

