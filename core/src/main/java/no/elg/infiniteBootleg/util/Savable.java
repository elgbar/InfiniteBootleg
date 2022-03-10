package no.elg.infiniteBootleg.util;

import com.google.protobuf.MessageOrBuilder;

/**
 * @author Elg
 */
public interface Savable<T extends MessageOrBuilder> {

  T save();
}
