package org.lineageos.zramstatus;
public class PlaybackDiagnosticsTest {
 public static void main(String[] args) throws Exception {
  String input="99.000 123 124 E ACodec: before window\n100.500 123 124 I ACodec: codec OMX.qcom.video.decoder.hevc 1920x1080\n101.000 456 457 E OMXNodeInstance: error 0x80001001 https://example.com/token\n101.000 456 457 E OMXNodeInstance: error 0x80001001 https://example.com/token\n101.500 123 124 I SecretTag: unrelated text\n111.000 123 124 I ACodec: after window\nmalformed";
  org.json.JSONArray out=PlaybackDiagnostics.extract(input,100000,110000,java.util.Set.of(123));
  if(out.length()!=2 || !out.getString(0).contains("selected_app") || !out.getString(1).contains("shared_media") || out.getString(1).contains("example.com") || !out.getString(1).contains("[address]"))throw new AssertionError("time, scope, deduplication or redaction failed");
  StringBuilder many=new StringBuilder();for(int i=0;i<100;i++)many.append("101.000 123 124 I ACodec: ").append(i).append("中".repeat(180)).append('\n');
  org.json.JSONArray bounded=PlaybackDiagnostics.extract(many.toString(),100000,110000,java.util.Set.of(123));int bytes=0;for(int i=0;i<bounded.length();i++)bytes+=bounded.getString(i).getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
  if(bounded.length()>48||bytes>8192||bounded.length()==0||!bounded.getString(bounded.length()-1).contains(": 99"))throw new AssertionError("bounds failed");
  if(!PlaybackDiagnostics.collect(null,100000,110000,java.util.Set.of()).getString("status").equals("unavailable"))throw new AssertionError("missing tool status failed");
  System.out.println("PASS: diagnostic time window, tag filtering, process scope, deduplication, address redaction, UTF-8 bound, missing-tool status");
 }
}
