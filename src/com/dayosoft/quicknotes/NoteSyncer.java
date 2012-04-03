package com.dayosoft.quicknotes;

public interface NoteSyncer {

	void process_new_records(Note[] notes_array);

	void process_updated_records(Note[] updated_notes_array);

}
