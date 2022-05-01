export const extractSongTitleFromCsvToArray = (str, delimiter = ",") => {
    const rows = str.split("\n");

    let res = [];

    rows.forEach(row => {
        res.push(row.split(delimiter)[7])
    })

    return res;
}

export const convertBatchResultToChar = (result) => {
    const rows = result.split('\n');
    let res = [];
    rows.forEach(row => {
        if (row.includes('not popular')) {
            res.push("✖️️")
        } else if (row.includes('popular')) {
            res.push("✔️")
        }
    })

    return res;
}

export const convertSingleResultToChar = (result) => {
    if (result.includes("0.0")) {
        return "✖️️";
    } else {
        return "✔️";
    }
}