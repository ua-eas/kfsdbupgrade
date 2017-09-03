package ua.utility.kfsdbupgrade.concurrent;

import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public interface MaintenanceDocConverter {

  ImmutableList<String> getDocumentIds();

  ImmutableMap<String, String> getDocuments(Iterable<String> ids);

  ImmutableMap<String, String> convertDocuments(Map<String, String> documents);

}
