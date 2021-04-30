package org.hyperledger.bpa.api;

import org.hyperledger.bpa.controller.api.wallet.WalletDocumentRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface DocumentMapper {

    DocumentMapper INSTANCE = Mappers.getMapper(DocumentMapper.class);

    @Mapping(source = "document", target = "documentData")
    MyDocumentAPI requestToDocumentApi(WalletDocumentRequest request);

}
