# Reunion150 Android
Android application for the Sesquicentennial Event to happen in 2016 at 
Carleton College.

Ongoing, it will support Reunion events.

## Configuration

### Get a google maps API key

The official API keys are in the webmaster@carleton.edu Google account. For development, use any key with a * permission. For production, use the one labeled 150th Android Key. If the signature of the app changes you will need to add additional package name/fingerprints to the API key restrictions.

### Place it in `res/values/google_maps_api.xml`, like this:

```
<resources>
    <string name="google_maps_key" translatable="false" templateMergeStrategy="preserve">
        Your Key Here
    </string>
</resources>
```

## Build Process

* `git clone https://github.com/Sesquicentennial/Android`
* open with Android Studio
* Try running the app!
