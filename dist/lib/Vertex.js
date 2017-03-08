function Vertex(uri, name, handle, id) {
// James Powell 5/16/2013
// 

  this.uri = uri;
  if ((name=="") || (name===undefined)) {
    this.label = handle;
  } else {
    this.label = name;
  }
  this.id = id;
} 

Vertex.prototype.getUri = function() {
    return this.uri;
}

Vertex.prototype.getLabel = function() {
    return this.label;
}

Vertex.prototype.getId = function() {
    return this.id;
}
