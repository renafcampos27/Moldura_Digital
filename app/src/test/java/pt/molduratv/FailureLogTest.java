package pt.molduratv;
import org.junit.Test;
import static org.junit.Assert.*;
public class FailureLogTest {
    @Test public void repeatedFailureMovesToFrontWithCountAndSurvivesRoundTrip() throws Exception {
        FailureLog log=new FailureLog(); log.add("foto.jpg","Leitura","Sem acesso",10);
        log.add("video.mp4","Codec","Não suportado",20); log.add("foto.jpg","Leitura","Sem acesso",30);
        FailureLog restored=FailureLog.decode(log.encode());
        assertEquals(2,restored.entries().size()); assertEquals("foto.jpg",restored.entries().get(0).file);
        assertEquals(2,restored.entries().get(0).count); assertEquals(30,restored.entries().get(0).time);
    }
    @Test public void historyIsBoundedAndTextCannotGrowWithoutLimit() {
        FailureLog log=new FailureLog();
        for(int i=0;i<75;i++) log.add("f"+i,"Codec","x".repeat(1000),i);
        assertEquals(50,log.entries().size()); assertEquals("f74",log.entries().get(0).file);
        assertEquals(300,log.entries().get(0).detail.length()); log.clear(); assertTrue(log.entries().isEmpty());
    }
    @Test public void corruptOrOversizedHistoryIsDiscarded() {
        assertTrue(FailureLog.decode("not json").entries().isEmpty());
        assertTrue(FailureLog.decode("x".repeat(100001)).entries().isEmpty());
    }
}
