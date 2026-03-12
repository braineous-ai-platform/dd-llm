package io.braineous.dd.llm.transaction.services;

import ai.braineous.rag.prompt.cgo.query.QueryRequest;
import io.braineous.dd.llm.transaction.model.TxStepRequest;

public interface TxQueryRequestTranslator {

    QueryRequest translate(TxStepRequest step);
}
