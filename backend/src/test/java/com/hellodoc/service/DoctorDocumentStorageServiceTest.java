package com.hellodoc.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class DoctorDocumentStorageServiceTest {
 @TempDir Path temp;
 private DoctorDocumentStorageService service(){return new DoctorDocumentStorageService(temp.toString());}
 @Test void acceptsPdfBySignatureRegardlessOfClaimedTypeAndStoresRandomly(){
  var stored=service().store(new MockMultipartFile("document","scan.exe","text/plain","%PDF-1.7 sample".getBytes()));
  assertEquals("application/pdf",stored.mediaType());assertEquals("scan.exe",stored.originalName());assertNotEquals("scan.exe",stored.storageKey());assertArrayEquals("%PDF-1.7 sample".getBytes(),service().read(stored.storageKey()));
 }
 @Test void acceptsPdfHeaderWithLeadingWhitespace(){byte[] bytes="\n %PDF-1.7 sample".getBytes();var stored=service().store(new MockMultipartFile("document","scan.pdf","application/pdf",bytes));assertEquals("application/pdf",stored.mediaType());}
 @Test void rejectsMissingDocument(){var ex=assertThrows(ResponseStatusException.class,()->service().store(null));assertTrue(ex.getReason().contains("obligatoire"));}
 @Test void rejectsUnsupportedContentEvenWithAllowedExtension(){assertThrows(ResponseStatusException.class,()->service().store(new MockMultipartFile("document","x.pdf","application/pdf","not a pdf".getBytes())));}
 @Test void rejectsOversizeBeforeReading(){var file=new MockMultipartFile("document","x.pdf","application/pdf",new byte[]{'%', 'P','D','F','-'});
  org.springframework.web.multipart.MultipartFile oversized=org.mockito.Mockito.mock(org.springframework.web.multipart.MultipartFile.class);org.mockito.Mockito.when(oversized.isEmpty()).thenReturn(false);org.mockito.Mockito.when(oversized.getSize()).thenReturn(DoctorDocumentStorageService.MAX_BYTES+1);
  assertThrows(ResponseStatusException.class,()->service().store(oversized)); }
}
