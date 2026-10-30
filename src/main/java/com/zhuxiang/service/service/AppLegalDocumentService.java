package com.zhuxiang.service.service;

import com.zhuxiang.service.dto.AppLegalDocumentDtos;

import java.util.List;

public interface AppLegalDocumentService {

    List<AppLegalDocumentDtos.DocumentView> listForAdmin(String operatorId);

    AppLegalDocumentDtos.DocumentView getForApp(String documentType);

    AppLegalDocumentDtos.DocumentView updateForAdmin(
            String documentType,
            AppLegalDocumentDtos.SaveDocumentRequest request,
            String operatorId
    );
}
