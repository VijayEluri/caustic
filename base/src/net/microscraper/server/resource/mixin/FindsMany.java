package net.microscraper.server.resource.mixin;

import java.io.IOException;

import net.microscraper.client.interfaces.JSONInterfaceArray;
import net.microscraper.client.interfaces.JSONInterfaceException;
import net.microscraper.client.interfaces.JSONInterfaceObject;
import net.microscraper.server.DeserializationException;
import net.microscraper.server.resource.FindMany;
import net.microscraper.server.resource.FindOne;

/**
 * Allows connections to a {@link FindMany} {@link Resource}.
 * @author john
 *
 */
public interface FindsMany {
	
	/**
	 * 
	 * @return An array of associated {@link FindMany} {@link Resource}s.
	 */
	public abstract FindMany[] getFindManys();
	
	/**
	 * A helper class to deserialize 
	 * interfaces of {@link FindsMany} using an inner constructor.
	 * Should only be instantiated inside {@link FindOne} or {@link SpawnedScraperExecutable}.
	 * @see FindOne
	 * @see SpawnedScraperExecutable
	 * @author john
	 *
	 */
	public static class Deserializer {
		private static final String KEY = "finds_many";
		
		/**
		 * Deserialize an {@link FindsMany} from a {@link JSONInterfaceObject}.
		 * @param jsonObject Input {@link JSONInterfaceObject} object.
		 * @return An {@link FindsMany} instance.
		 * @throws DeserializationException If this is not a valid JSON serialization of
		 * a {@link FindsMany}.
		 * @throws IOException If there is an error loading one of the references.
		 */
		public static FindsMany deserialize(JSONInterfaceObject jsonObject)
					throws DeserializationException, IOException {
			try {
				final FindMany[] findManys;
				if(jsonObject.has(KEY)) {
					JSONInterfaceArray array = jsonObject.getJSONArray(KEY);
					findManys = new FindMany[array.length()];
					for(int i = 0 ; i < findManys.length ; i ++) {
						findManys[i] = new FindMany(array.getJSONObject(i));
					}
				} else {
					findManys = new FindMany[0];
				}
				return new FindsMany() {
					public FindMany[] getFindManys() {
						return findManys;
					}
				};
			} catch(JSONInterfaceException e) {
				throw new DeserializationException(e, jsonObject);
			}
		}
	}
}
