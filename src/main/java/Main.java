import melts.MELTSinput;
import melts.MELTSoutput;
import melts.ObjectFactory;
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
import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String[] args) throws JAXBException, IOException {
//        List<String> phases = Arrays.asList("aegirine",
//                "aenigmatite",
//                "alloy-liquid",
//                "alloy-solid",
//                "amphibole",
//                "apatite",
//                "biotite",
//                "clinopyroxene",
//                "corundum",
//                "cristobalite",
//                "cummingtonite",
//                "fayalite",
//                "feldspar",
//                "garnet",
//                "hornblende",
//                "kalsilite",
//                "leucite",
//                "melilite",
//                "muscovite",
//                "nepheline",
//                "olivine",
//                "ortho-oxide",
//                "orthopyroxene",
//                "perovskite",
//                "quartz",
//                "rhm-oxide",
//                "rutile",
//                "sillimanite",
//                "sphene",
//                "spinel",
//                "tridymite",
//                "water",
//                "whitlockite");
        List<String> excludedPhases = Arrays.asList("leucite", "amphibole");
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
        int[] t_var = {1000, 700, 2};
        int[] p_var = {4000, 250, 250};
        double fo2_var = 0.00;
        double[] h_var = {0, 0, 0.2};
        double[] v_var = {0, 0, 0};
        double[] s_var = {322.2, 0, 0};
        double[] liquid_thermo = {95.87,2.22,-1558.22,-1307.87,243.02,128.61,43.28,1.42e-4,1.71e-5};
        double[] system_thermo = {109.85,1.90,-1808.25,-1492.02,306.98,159.60,57.78,4.06e-4,7.89e-5};
        double p1 = convert_p(p_var[0], PressureUnit.MPA, PressureUnit.BAR);
        double p2 = convert_p(p_var[1], PressureUnit.MPA, PressureUnit.BAR);
        if (p1 < p2) {
            double pTemp = p2;
            p2 = p1;
            p1 = pTemp;
        }
        double t1 = convert_t(t_var[0], TemperatureUnit.C, TemperatureUnit.K);
        double t2 = convert_t(t_var[1], TemperatureUnit.C, TemperatureUnit.K);
        ;
        if (t1 < t2) {
            double tTemp = t2;
            t2 = t1;
            t1 = tTemp;
        }
        double deltaP = p_var[2];
        double deltaT = t_var[2];
        if (deltaP == 0) deltaP = 1;
        int numRunsP = (int) (Math.abs(p1 - p2) / deltaP + 1);
        int numRunsT = (int) (Math.abs(t1 - t2) / deltaT + 1);
        int numRuns = Math.max(numRunsP, numRunsT);
        int k = 0;
        int newK = 0;
        String sessionID = "";
        System.out.println(numRuns);
        while (k < numRuns) {
            if (k != 0) {
                in = factory.createMELTSinput();
                in.setSessionID(sessionID);
            }
            if (k % numRunsT == 0) {
                in.setInitialize(cl);
            }
            double t = t1 - ((k % numRunsT) * deltaT);
            double p = p1 - ((double)k / numRunsP) * deltaP;
            MELTSinput.Constraints constraints = factory.createMELTSinputConstraints();
            MELTSinput.Constraints.SetTP tp = factory.createMELTSinputConstraintsSetTP();
            tp.setFo2Path("fo2");
            tp.setFo2Offset(fo2_var);
            tp.setInitialT(t);
            tp.setInitialP(p);
            constraints.setSetTP(tp);
            in.setConstraints(constraints);
            in.setCalculationMode("equilibrate");
            in.getSuppressPhase().addAll(excludedPhases);
            in.getFractionationMode().add("fractionateNone");
            if (k % numRunsT == 0) {
                in.setCalculationMode("findWetLiquidus");
                MELTSoutput o = submit(in);
                double newT = o.getTemperature();
                System.out.println(t - newT);
                newK = (int) ((t - newT) / deltaT);
                newT = t - newK * deltaT;
                tp.setInitialT(newT);
                in.setCalculationMode("equilibrate");
                System.out.println("Liquidus found: "+Math.round(newT));
                System.out.println("Simulations skipped: "+newK);
            }
            MELTSoutput o = submit(in);
            sessionID = o.getSessionID();
            if (k % numRunsT == 0) {
                k += newK;
            } else if(liquid_thermo[0]/system_thermo[0] < 0.1) {
                k += numRunsT - (k%numRunsT)-1;
                System.out.println("Liquid < 10 wt. %, moving on to next P...");
                System.out.println("Simulations skipped: "+ (numRunsT - (k % numRunsT)));
            }
        }

    }

    enum PressureUnit {
        BAR, KBAR, MPA
    }

    enum TemperatureUnit {
        K, C
    }

    public static double convert_t(double oldT, TemperatureUnit oldUnit, TemperatureUnit newUnit) {
        if (oldUnit != TemperatureUnit.K) oldT += 273.15;
        if (newUnit != TemperatureUnit.K) oldT -= 273.15;
        return oldT;
    }

    public static double convert_p(double oldP, PressureUnit oldUnit, PressureUnit newUnit) {
        switch (oldUnit) {
            case BAR:
                oldP /= 10;
                break;
            case KBAR:
                oldP *= 100;
                break;
            case MPA:
                break;
        }
        switch (oldUnit) {
            case BAR:
                oldP *= 10;
                break;
            case KBAR:
                oldP /= 100;
                break;
            case MPA:
                break;
        }
        return oldP;
    }

    public static MELTSoutput submit(MELTSinput in) throws JAXBException, IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        JAXBContext jaxbContext = JAXBContext.newInstance(in.getClass());

        Marshaller marshaller = jaxbContext.createMarshaller();

        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.marshal(in, stream);
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpPost httppost = new HttpPost("http://209.180.202.74:8080/multiMELTSWSBxApp/Compute");
        httppost.setEntity(new StringEntity(stream.toString()));
        httppost.setHeader("Content-Type", "text/xml");
        httppost.setHeader("Data-Type", "xml");
//Execute and get the response.
        CloseableHttpResponse response = httpclient.execute(httppost);
        HttpEntity entity = response.getEntity();

        if (entity != null) {
            try (InputStream instream = entity.getContent()) {
                JAXBContext out = JAXBContext.newInstance(MELTSoutput.class);
                return (MELTSoutput) out.createUnmarshaller().unmarshal(instream);
            }
        }
        return null;
    }
}
