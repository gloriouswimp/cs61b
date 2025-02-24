# Gitlet Design Document

**Name**:

## Skeleton

基本设计思路：运行(runtime)和存储(persistence)分开思考

persistence 的任务在运行开始、运行完成时解决，解决文件存储问题

runtime 解决 command 问题

## Classes and Data Structures

### Blob

表达单个文件，由 SHA-1 生成，能唯一表示文件内容

当文件内容一致时，认为两个 blob 一致。意味着 blob 的 hash 编码可以设计为：

```java
Hash(Filename, content);
```

需要注意的是，按照这种设计，commit 就需要记录每个文件的文件名，并通过文件名指向对应的 blobID

#### content of a file

直接读取内容，并使用 SHA-1 进行 hash 编码

#### Instance Variables

1. `String id`
2. `String content`
3. `File filePath`

#### Method

1. `public Blob(String content)`

2. `public void saveBlob()`

3. `public static String readContentFromID(String ID)`

### Tree

指向单个 commit 所包含的所有文件。

由于 Gitlet 没有子目录，Tree 没有子树，一级结点均为叶子结点

可以直接使用 HasMap 结构实现

> Warning:
> 
> HashMap 在序列化时是无序的，因此同一个 Commit 通过序列化后的结果可能是不一样的，故不能使用 HashMap 进行存储，而应当使用 TreeMap

### Commit

Commit 之间应该存在结点连接关系

**核心：commit 之间的关系是什么样的**

Q1：是否需要设置双向连接

    不需要，Commit 只需要记得父节点，不需要记得子节点。用户应当自己记忆子节点以解决 reset 的问题。当用户指定 commit id 时，只需要在 persistence 中查找该 commit 文件

Q2：连接结点数怎么确定

    只要 2 个父节点

#### Date

By the way, you’ll find that the Java classes `java.util.Date` and `java.util.Formatter` are useful for getting and formatting times.

### Repository

Repository 相当于 local repo，存储了所有 commit 以及 commit 之间的关系，并且存储了当前 commit 的信息，用 HEAD 表达

#### Head

用来指向当前所处 commit，单纯是一个 commit 的指针，但同时，Head 也应当记录 branch 的名称，或者说每个 Head 就是一个 branch 

1. **Q：Head 指向的 commit 是否是叶子结点？**

一个个命令来检查，检查命令是否改变 Head 指向的对象

实际运行来看，Head 不一定是叶子结点，reset 命令可以将 Head 指向过去的结点

如果用户记得 reset 前的 commit id，那么实际上以前的 commit 还是存在的

2. Repository 和 Head 应该怎么交互

Repository 应当能给出当前 Head 以及所有 Head

#### Stage Area

Stage Area 应当这么看待：

当 repo 刚切换到某一 branch 时，Stage Area 是清空的，应当当作和 Commit 一样的文件结构。只有 rm/add 命令使用后，Stage Area 才会增删文件。

可以将 Stage Area 当作一个 Commit，add 操作相当于对 blobMap 进行了一次 additem 操作，rm 相当于进行了一次 rmitem 操作

但为了节省空间，stage area 实际上只记录和 commit 之间的变化（差异）

> dangling object
> 
> 显然，在反复 add 和 rm 操作中，不被任何引用指向的 blob 是可能出现的，称为 danglling object。gitlet 不讨论这一问题的解决，git 是使用 `gc` 来解决这一存储消耗问题的。

**design**

由于需要记录 add 和 rm 两种对象，分别用两个文件记录 `addFile` 和 `removeFile`

> warning:
> 
> `addFile` 和 `removeFile` 并不是独立的，操作时应当同时对两者操作

数据结构：

`addFile` 可以设计为一个 map，从文件名到 objectID

`removeFile` 可以设计为文件名的 List，记录要删除的文件名

## The Commands

### Common Failure cases

- If a user doesn’t input any arguments, print the message `Please enter a command.` and exit.

- If a user inputs a command that doesn’t exist, print the message `No command with that name exists.` and exit.

- If a user inputs a command with the wrong number or format of operands, print the message `Incorrect operands.` and exit.

- If a user inputs a command that requires being in an initialized Gitlet working directory (i.e., one containing a `.gitlet` subdirectory), but is not in such a directory, print the message `Not in an initialized Gitlet directory.`

### init

A Gitlet system is considered “initialized” in a particular location if it has a `.gitlet` directory there

### add

只加入单个文件。

复杂度：与 staged area 文件数量成线性关系，与 Commit 内文件数成 `O(lg N)` 关系

> comment:
> 
> `O(lg N)` 源自于 TreeMap 的搜索复杂度



### commit

### rm

### log

只展示 first-parent commit 的内容

### global-log

### find

### status

### checkout

A case of this command will change the Head pointer similar to *switch* in git.

### branch

### rm-branch

### reset

This command will change the Head pointer

### merge

## Algorithms

### Hashing

## Persistence
