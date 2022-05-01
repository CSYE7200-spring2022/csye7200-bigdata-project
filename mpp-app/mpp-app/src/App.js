import './App.css';
import React, {useState} from "react";
import {convertBatchResultToChar, convertSingleResultToChar, extractSongTitleFromCsvToArray} from "./util/util";
import axios from "axios";

import List from './components/List';

function App() {
    const serverUrl = "127.0.0.1:9000/"
    const [batchResult, setBatchResult] = useState({"lr": [], "rf": []});
    const [singleResult, setSingleResult] = useState("");
    const [singleInput, setSingleInput] = useState({
        "artist_familiarity": 0.3318741464569221,
        "artist_hotttnesss": 0.2771195134672702,
        "artist_latitude": 8.4177,
        "artist_longitude": -80.11278,
        "danceability": 0,
        "duration": 459.31057,
        "end_of_fade_in": 0.379,
        "energy": 0,
        "key": 4,
        "key_confidence": 0.566,
        "loudness": -14.117,
        "mode": 1,
        "mode_confidence": 0.542,
        "start_of_fade_out": 451.1,
        "tempo": 100.394,
        "time_signature": 4,
        "time_signature_confidence": 0,
        "artist_terms": [
            "latin jazz",
            "future jazz",
            "brazilian jazz",
            "piano blues",
            "broken beat",
            "acid jazz",
            "smooth jazz",
            "jazz",
            "blues",
            "electronic",
            "latin",
            "contemporary jazz",
            "piano",
            "classic",
            "fusion",
            "big band",
            "post-bop",
            "modern jazz",
            "european",
            "panama",
            "afro-cuban jazz"
        ],
        "year": 2008
    });
    const [model, setModel] = useState(false);

    const batchInference = (e, method) => {
        const file = e.target.files[0];
        const reader = new FileReader();
        reader.readAsText(file);
        let songTitle = [];
        reader.onload = (event) => {
            songTitle = extractSongTitleFromCsvToArray(event.target.result);

            const form = new FormData();
            form.append("csv", file);
            axios({
                method: "post",
                url: `http://${serverUrl}spark/infer_batch/${method}`,
                data: form,
                headers: {"Content-Type": "multipart/form-data", "Accept": "*/*"}
            }).then((res) => {
                const res1 = convertBatchResultToChar(res.data);
                console.log(res1);
                setBatchResult(
                    (previous) => {
                        if (method === "lr") {
                            previous.lr = res1;
                        } else {
                            previous.rf = res1;
                        }
                        return {...previous}
                    }
                )
            }).catch(function (error) {
            });
        }
    }

    const singleInference = async (e) => {
        e.preventDefault();

        let m = "lr";
        if (model) m = "rf";

        console.log(singleInput);

        axios.post(
            `http://${serverUrl}spark/infer/${m}`,
            singleInput
        ).then((res) => {
            let res1 = convertSingleResultToChar(res.data);
            setSingleResult(res1);
        }).catch(function (error) {
        });
    }

    return (
        <div className="Music Popularity Prediction">
            <h1> Music Popularity Prediction </h1>

            <h2> Single Song Record </h2>

            <p>Result: {singleResult}</p>

            <form id="info_form" onSubmit={singleInference}>
                <fieldset>
                    <label><b>Model</b></label>
                    <br/>
                    <label id="via_avoid_select">
                        <select onChange={(e) => {
                            if (e.target.value.includes("Linear Regression")) {
                                setModel(false);
                            } else {
                                setModel(true);
                            }
                        }}>
                            <option>Linear Regression</option>
                            <option>Random Forest</option>
                        </select>
                    </label>
                    <br/>
                    <label><b>Artist</b></label>
                    <br/>
                    <label htmlFor="form_title">familiarity </label>
                    <input type="number" id="form_title" step="0.0000001" required
                           onChange={(e) => setSingleInput((previous) => {
                               previous["artist_familiarity"] = Number(e.target.value)
                               return previous;
                           })}/>
                    <br/>
                    <label htmlFor="form_title">latitude </label>
                    <input type="number" id="form_title" step="0.000001" onChange={(e) => setSingleInput((previous) => {
                        previous["artist_latitude"] = Number(e.target.value)
                        return previous;
                    })}/>
                    <br/>
                    <label htmlFor="form_title">longitude </label>
                    <input type="number" id="form_title" step="0.000001" onChange={(e) => setSingleInput((previous) => {
                        previous["artist_longitude"] = Number(e.target.value)
                        return previous;
                    })}/>
                    <br/>
                    <label htmlFor="form_description">terms</label>
                    <br/>
                    <textarea cols="25" rows="5" wrap="hard" id="form_description" required
                              placeholder="Please use comma as delimiter. (e.g. brazilian jazz, blues)"
                              onChange={(e) => {
                                  setSingleInput((previous) => {
                                      const words = e.target.value.split(",");
                                      let wordList = [];
                                      words.forEach(word => {
                                          wordList.push(word);
                                      })
                                      previous["artist_terms"] = wordList;
                                      return previous;
                                  })
                              }}/>
                    <br/>
                    <label><b>Song</b></label>
                    <br/>
                    <label htmlFor="danceability">danceability </label>
                    <input type="checkbox" id="danceability" defaultChecked={false} onChange={(e) => setSingleInput((previous) => {
                        previous["danceability"] = Number(e.target.checked)
                        return previous;
                    })}/>
                    <br/>
                    <label htmlFor="form_title">duration </label>
                    <input type="number" id="form_title" step="0.00001" required
                           onChange={(e) => setSingleInput((previous) => {
                               previous["duration"] = Number(e.target.value)
                               return previous;
                           })}/>
                    <br/>
                    <label htmlFor="form_title">end of fade in </label>
                    <input type="number" id="form_title" step="0.001" onChange={(e) => setSingleInput((previous) => {
                        previous["end_of_fade_in"] = Number(e.target.value)
                        return previous;
                    })}/>
                    <br/>
                    <label htmlFor="form_title">energy </label>
                    <input type="number" id="form_title" onChange={(e) => setSingleInput((previous) => {
                        previous["energy"] = Number(e.target.value)
                        return previous;
                    })}/>
                    <br/>
                    <label htmlFor="form_title">key </label>
                    <input type="number" id="form_title" required onChange={(e) => setSingleInput((previous) => {
                        previous["key"] = Number(e.target.value)
                        return previous;
                    })}/>
                    <br/>
                    <label htmlFor="form_title">key confidence </label>
                    <input type="number" id="form_title" step="0.001" onChange={(e) => setSingleInput((previous) => {
                        previous["key_confidence"] = Number(e.target.value)
                        return previous;
                    })}/>
                    <br/>
                    <label htmlFor="form_title">loudness </label>
                    <input type="number" id="form_title" required step="0.001"
                           onChange={(e) => setSingleInput((previous) => {
                               previous["loudness"] = Number(e.target.value)
                               return previous;
                           })}/>
                    <br/>
                    <label htmlFor="form_title">mode </label>
                    <input type="number" id="form_title" required onChange={(e) => setSingleInput((previous) => {
                        previous["mode"] = Number(e.target.value)
                        return previous;
                    })}/>
                    <br/>
                    <label htmlFor="form_title">mode confidence </label>
                    <input type="number" id="form_title" step="0.001" onChange={(e) => setSingleInput((previous) => {
                        previous["mode_confidence"] = Number(e.target.value)
                        return previous;
                    })}/>
                    <br/>
                    <label htmlFor="form_title">start of fade out </label>
                    <input type="number" id="form_title" step="0.001" onChange={(e) => setSingleInput((previous) => {
                        previous["start_of_fade_out"] = Number(e.target.value)
                        return previous;
                    })}/>
                    <br/>
                    <label htmlFor="form_title">tempo </label>
                    <input type="number" id="form_title" step="0.001" onChange={(e) => setSingleInput((previous) => {
                        previous["tempo"] = Number(e.target.value)
                        return previous;
                    })}/>
                    <br/>
                    <label htmlFor="form_title">time signature </label>
                    <input type="number" id="form_title" onChange={(e) => setSingleInput((previous) => {
                        previous["time_signature"] = Number(e.target.value)
                        return previous;
                    })}/>
                    <br/>
                    <label htmlFor="form_title">time signature confidence </label>
                    <input type="number" id="form_title" step="0.001" onChange={(e) => setSingleInput((previous) => {
                        previous["time_signature_confidence"] = Number(e.target.value)
                        return previous;
                    })}/>
                    <br/>
                    <label htmlFor="form_title">year </label>
                    <input type="number" id="form_title" min={1500} max={2999}
                           onChange={(e) => setSingleInput((previous) => {
                               previous["year"] = Number(e.target.value)
                               return previous;
                           })}/>
                    <br/>
                </fieldset>

                <div className="submit_btn">
                    <button type="submit" id="form_submit_btn">Submit</button>
                </div>
            </form>

            <h2> Multiple Song Records </h2>

            <div>
                <h3>Linear Regression (Batch)</h3>
                <input type="file" id="batch_csv_file" accept=".csv" onChange={(e) => {
                    batchInference(e, 'lr')
                }}/>
                <div className="batch_result_list">
                    <ol>
                        {batchResult['lr'].map((a) => {
                            return (
                                <li>{a}</li>
                            )
                        })}
                    </ol>
                </div>
            </div>

            <div>
                <h3>Random Forest (Batch)</h3>
                <input type="file" id="batch_csv_file" accept=".csv" onChange={(e) => {
                    batchInference(e, 'rf')
                }}/>
                <div className="batch_result_list">
                    <ol>
                        {batchResult['rf'].map((a) => {
                            return (
                                <li>{a}</li>
                            )
                        })}
                    </ol>
                </div>
            </div>

        </div>
    );
}

export default App;
