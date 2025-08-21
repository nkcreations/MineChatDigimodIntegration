package cn.tropicalalgae.minechat.common.personality;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Personality {
    @SerializedName("species")
    public String species;

    @SerializedName("persona")
    public Persona persona;

    @SerializedName("tabues")
    public List<String> tabues;

    public static class Persona {
        @SerializedName("rasgos")
        public List<String> rasgos;

        @SerializedName("estilo")
        public String estilo;

        @SerializedName("muletillas")
        public List<String> muletillas;
    }
}
