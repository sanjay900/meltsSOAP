import melts.MELTSinput;
import melts.MELTSoutput;
import melts.ObjectFactory;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class Main {
    public static void main(String[] args) throws JAXBException, IOException {
        ObjectFactory factory = new ObjectFactory();
        MELTSinput in = factory.createMELTSinput();
        in.setTitle("Test");
        in.setSessionID("test-k");
        MELTSinput.Initialize cl = factory.createMELTSinputInitialize();
        cl.setSiO2(75.925);
        cl.setTiO2(0.286);
        cl.setAl2O3(13.213);
        cl.setFe2O3(0.509);
        cl.setFeO(1.603);
        cl.setMnO(0.015);
        cl.setMgO(0.266);
        cl.setCaO(4.435);
        cl.setNa2O(2.942);
        cl.setK2O(1.539);
        cl.setH2O(10.0);
        in.setInitialize(cl);
        in.setCalculationMode("equilibrate");
        MELTSinput.Constraints constraints = factory.createMELTSinputConstraints();
        MELTSinput.Constraints.SetTP tp = factory.createMELTSinputConstraintsSetTP();
        tp.setFo2Path("fo2");
        tp.setInitialT(1000);
        tp.setInitialP(4000);
        constraints.setSetTP(tp);
        in.setConstraints(constraints);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        JAXBContext jaxbContext = JAXBContext.newInstance(in.getClass());

        Marshaller marshaller = jaxbContext.createMarshaller();

        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.marshal(in, stream);
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpPost httppost = new HttpPost("http://thermofit.ofm-research.org:8080/multiMELTSWSBxApp/Compute");
        httppost.setEntity(new StringEntity(stream.toString()));
        httppost.setHeader("Content-Type", "text/xml");
        httppost.setHeader("Data-Type", "xml");
//Execute and get the response.
        CloseableHttpResponse response = httpclient.execute(httppost);
        HttpEntity entity = response.getEntity();

        if (entity != null) {
            try (InputStream instream = entity.getContent()) {
                JAXBContext out = JAXBContext.newInstance(MELTSoutput.class);
                MELTSoutput output = (MELTSoutput) out.createUnmarshaller().unmarshal(instream);
                System.out.println(output.getPressure());
            }
        }

    }
}
